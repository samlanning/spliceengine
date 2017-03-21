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

import com.splicemachine.orc.StreamDescriptor;
import com.splicemachine.orc.metadata.ColumnEncoding;
import com.splicemachine.orc.stream.BooleanStream;
import com.splicemachine.orc.stream.StreamSource;
import com.splicemachine.orc.stream.StreamSources;
import org.apache.spark.memory.MemoryMode;
import org.apache.spark.sql.execution.vectorized.ColumnVector;
import org.apache.spark.sql.types.DataType;
import org.apache.spark.sql.types.StructType;
import org.joda.time.DateTimeZone;
import scala.collection.JavaConversions;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.util.List;

import static com.splicemachine.orc.metadata.Stream.StreamKind.PRESENT;
import static com.splicemachine.orc.reader.StreamReaders.createStreamReader;
import static com.splicemachine.orc.stream.MissingStreamSource.missingStreamSource;
import static com.google.common.base.MoreObjects.toStringHelper;
import static java.util.Objects.requireNonNull;

public class StructStreamReader
        extends AbstractStreamReader {
    private final StreamDescriptor streamDescriptor;

    private final StreamReader[] structFields;


    @Nonnull
    private StreamSource<BooleanStream> presentStreamSource = missingStreamSource(BooleanStream.class);
    @Nullable
    private BooleanStream presentStream;

    private boolean rowGroupOpen;

    public StructStreamReader(StreamDescriptor streamDescriptor, DateTimeZone hiveStorageTimeZone)
    {
        this.streamDescriptor = requireNonNull(streamDescriptor, "stream is null");

        List<StreamDescriptor> nestedStreams = streamDescriptor.getNestedStreams();
        this.structFields = new StreamReader[nestedStreams.size()];
        for (int i = 0; i < nestedStreams.size(); i++) {
            StreamDescriptor nestedStream = nestedStreams.get(i);
            this.structFields[i] = createStreamReader(nestedStream, hiveStorageTimeZone);
        }
    }

    @Override
    public void prepareNextRead(int batchSize)
    {
        readOffset += nextBatchSize;
        nextBatchSize = batchSize;
    }

    @Override
    public ColumnVector readBlock(DataType type, ColumnVector vector)
            throws IOException {
        StructType structType = (StructType) type;
        List<StructType> structElements = JavaConversions.bufferAsJavaList(structType.seq().toBuffer());
        if (!rowGroupOpen) {
            openRowGroup();
        }

        if (readOffset > 0) {
            if (presentStream != null) {
                // skip ahead the present bit reader, but count the set bits
                // and use this as the skip size for the field readers
                readOffset = presentStream.countBitsSet(readOffset);
            }
            for (StreamReader structField : structFields) {
                structField.prepareNextRead(readOffset);
            }
        }

        boolean[] nullVector = new boolean[nextBatchSize];
        if (presentStream == null) { // No Nulls
            for (int i = 0; i < structElements.size(); i++) {
                StreamReader structField = structFields[i];
                structField.prepareNextRead(nextBatchSize);
                ColumnVector childVector = vector.getChildColumn(i);
                // Mark Child Vectors null positions based on interleaving
                int j = 0;
                for (int k=0;k<nextBatchSize;k++) {
                    while (vector.isNullAt(k+j)) {
                        childVector.putNull(k+j); // Set Child Vector to null at that position
                        if (i == 0) { // first element
                            vector.appendStruct(false);
                            vector.putNull(k+j);
                        }
                        j++;
                    }
                    if (i==0)
                        vector.appendStruct(false);
                }
                structField.readBlock(structType.fields()[i].dataType(),childVector);
            }
        }
        else {
            int nullValues = presentStream.getUnsetBits(nextBatchSize, nullVector);
            if (nullValues != nextBatchSize) {
                int lastStructElement = structElements.size()-1;
                for (int i = 0; i < structElements.size(); i++) {
                    StreamReader structField = structFields[i];
                    structField.prepareNextRead(nextBatchSize-nullValues);
                    ColumnVector childVector = vector.getChildColumn(i);
                    // Mark Child Vectors null positions based on interleaving
                    int j = 0;
                    for (int k=0;k<nextBatchSize;k++) {
                        while (vector.isNullAt(k+j)) {
                            childVector.putNull(k+j); // Set Child Vector to null at that position
                            if (i == lastStructElement) { // first element
                                vector.appendStruct(false);
                                vector.putNull(k+j);
                            }
                            j++;
                        }
                        if (i==lastStructElement)
                            vector.appendStruct(false);
                        if (nullVector[k]) {
                            childVector.putNull(k+j);
                            if (i==lastStructElement)
                                vector.putNull(k+j);
                        }
                    }
                    structField.readBlock(structType.fields()[i].dataType(),childVector);
                }

            }
            else {
                int j = 0;
                for (int k=0;k<nextBatchSize;k++) {
                    while (vector.isNullAt(k+j)) {
                        vector.appendStruct(false);
                        vector.putNull(k+j);
                        j++;
                    }
                    vector.appendStruct(false);
                    vector.putNull(k+j);
                }
            }
        }

        readOffset = 0;
        nextBatchSize = 0;
        return vector;
    }

    private void openRowGroup()
            throws IOException
    {
        presentStream = presentStreamSource.openStream();

        rowGroupOpen = true;
    }

    @Override
    public void startStripe(StreamSources dictionaryStreamSources, List<ColumnEncoding> encoding)
            throws IOException
    {
        presentStreamSource = missingStreamSource(BooleanStream.class);

        readOffset = 0;
        nextBatchSize = 0;

        presentStream = null;

        rowGroupOpen = false;

        for (StreamReader structField : structFields) {
            structField.startStripe(dictionaryStreamSources, encoding);
        }
    }

    @Override
    public void startRowGroup(StreamSources dataStreamSources)
            throws IOException
    {
        presentStreamSource = dataStreamSources.getStreamSource(streamDescriptor, PRESENT, BooleanStream.class);

        readOffset = 0;
        nextBatchSize = 0;

        presentStream = null;

        rowGroupOpen = false;

        for (StreamReader structField : structFields) {
            structField.startRowGroup(dataStreamSources);
        }
    }

    @Override
    public String toString()
    {
        return toStringHelper(this)
                .addValue(streamDescriptor)
                .toString();
    }
}