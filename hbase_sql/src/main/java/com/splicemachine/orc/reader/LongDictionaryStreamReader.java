/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.splicemachine.orc.reader;

import com.splicemachine.orc.OrcCorruptionException;
import com.splicemachine.orc.StreamDescriptor;
import com.splicemachine.orc.metadata.ColumnEncoding;
import com.splicemachine.orc.stream.BooleanStream;
import com.splicemachine.orc.stream.LongStream;
import com.splicemachine.orc.stream.StreamSource;
import com.splicemachine.orc.stream.StreamSources;
import org.apache.spark.memory.MemoryMode;
import org.apache.spark.sql.execution.vectorized.ColumnVector;
import org.apache.spark.sql.types.DataType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import static com.splicemachine.orc.metadata.Stream.StreamKind.*;
import static com.splicemachine.orc.stream.MissingStreamSource.missingStreamSource;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class LongDictionaryStreamReader
        extends AbstractStreamReader {
    private final StreamDescriptor streamDescriptor;

    @Nonnull
    private StreamSource<BooleanStream> presentStreamSource = missingStreamSource(BooleanStream.class);
    @Nullable
    private BooleanStream presentStream;
    private boolean[] nullVector = new boolean[0];

    @Nonnull
    private StreamSource<LongStream> dictionaryDataStreamSource = missingStreamSource(LongStream.class);
    private int dictionarySize;
    @Nonnull
    private long[] dictionary = new long[0];

    @Nonnull
    private StreamSource<BooleanStream> inDictionaryStreamSource = missingStreamSource(BooleanStream.class);
    @Nullable
    private BooleanStream inDictionaryStream;
    private boolean[] inDictionary = new boolean[0];

    @Nonnull
    private StreamSource<LongStream> dataStreamSource;
    @Nullable
    private LongStream dataStream;
    private long[] dataVector = new long[0];

    private boolean dictionaryOpen;
    private boolean rowGroupOpen;

    public LongDictionaryStreamReader(StreamDescriptor streamDescriptor)
    {
        this.streamDescriptor = requireNonNull(streamDescriptor, "stream is null");
    }

    @Override
    public void prepareNextRead(int batchSize)
    {
        readOffset += nextBatchSize;
        nextBatchSize = batchSize;
    }

    @Override
    public ColumnVector readBlock(DataType type, ColumnVector vector)
            throws IOException
    {
        if (!rowGroupOpen) {
            openRowGroup();
        }

        if (readOffset > 0) {
            if (presentStream != null) {
                // skip ahead the present bit reader, but count the set bits
                // and use this as the skip size for the length reader
                readOffset = presentStream.countBitsSet(readOffset);
            }

            if (inDictionaryStream != null) {
                inDictionaryStream.skip(readOffset);
            }

            if (readOffset > 0) {
                if (dataStream == null) {
                    throw new OrcCorruptionException("Value is not null but data stream is not present");
                }
                dataStream.skip(readOffset);
            }
        }

        if (nullVector.length < nextBatchSize) {
            nullVector = new boolean[nextBatchSize];
        }
        if (dataVector.length < nextBatchSize) {
            dataVector = new long[nextBatchSize];
        }
        if (presentStream == null) {
            if (dataStream == null) {
                throw new OrcCorruptionException("Value is not null but data stream is not present");
            }
            Arrays.fill(nullVector, false);
            dataStream.nextLongVector(nextBatchSize, dataVector);
        }
        else {
            int nullValues = presentStream.getUnsetBits(nextBatchSize, nullVector);
            if (nullValues != nextBatchSize) {
                if (dataStream == null) {
                    throw new OrcCorruptionException("Value is not null but data stream is not present");
                }
                dataStream.nextLongVector(nextBatchSize, dataVector, nullVector);
            }
        }

        if (inDictionary.length < nextBatchSize) {
            inDictionary = new boolean[nextBatchSize];
        }
        if (inDictionaryStream == null) {
            Arrays.fill(inDictionary, true);
        }
        else {
            inDictionaryStream.getSetBits(nextBatchSize, inDictionary, nullVector);
        }

        for (int i = 0, j = 0; i < nextBatchSize; i++) {
            while (vector.isNullAt(i+j)) {
                vector.appendNull();
                j++;
            }
            if (nullVector[i]) {
                vector.appendNull();
            }
            else if (inDictionary[i]) {
                vector.appendLong(dictionary[((int) dataVector[i])]);
            }
            else {
                vector.appendLong(dataVector[i]);
            }
        }

        readOffset = 0;
        nextBatchSize = 0;

        return vector;
    }

    private void openRowGroup()
            throws IOException
    {
        // read the dictionary
        if (!dictionaryOpen && dictionarySize > 0) {
            if (dictionary.length < dictionarySize) {
                dictionary = new long[dictionarySize];
            }

            LongStream dictionaryStream = dictionaryDataStreamSource.openStream();
            if (dictionaryStream == null) {
                throw new OrcCorruptionException("Dictionary is not empty but data stream is not present");
            }
            dictionaryStream.nextLongVector(dictionarySize, dictionary);
        }
        dictionaryOpen = true;

        presentStream = presentStreamSource.openStream();
        inDictionaryStream = inDictionaryStreamSource.openStream();
        dataStream = dataStreamSource.openStream();

        rowGroupOpen = true;
    }

    @Override
    public void startStripe(StreamSources dictionaryStreamSources, List<ColumnEncoding> encoding)
            throws IOException
    {
        dictionaryDataStreamSource = dictionaryStreamSources.getStreamSource(streamDescriptor, DICTIONARY_DATA, LongStream.class);
        dictionarySize = encoding.get(streamDescriptor.getStreamId()).getDictionarySize();
        dictionaryOpen = false;

        inDictionaryStreamSource = missingStreamSource(BooleanStream.class);
        presentStreamSource = missingStreamSource(BooleanStream.class);
        dataStreamSource = missingStreamSource(LongStream.class);

        readOffset = 0;
        nextBatchSize = 0;

        presentStream = null;
        inDictionaryStream = null;
        dataStream = null;

        rowGroupOpen = false;
    }

    @Override
    public void startRowGroup(StreamSources dataStreamSources)
            throws IOException
    {
        presentStreamSource = dataStreamSources.getStreamSource(streamDescriptor, PRESENT, BooleanStream.class);
        inDictionaryStreamSource = dataStreamSources.getStreamSource(streamDescriptor, IN_DICTIONARY, BooleanStream.class);
        dataStreamSource = dataStreamSources.getStreamSource(streamDescriptor, DATA, LongStream.class);

        readOffset = 0;
        nextBatchSize = 0;

        presentStream = null;
        inDictionaryStream = null;
        dataStream = null;

        rowGroupOpen = false;
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(streamDescriptor)
                .toString();
    }
}