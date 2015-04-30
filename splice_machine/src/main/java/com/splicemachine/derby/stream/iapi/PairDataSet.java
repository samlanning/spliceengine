package com.splicemachine.derby.stream.iapi;

import com.google.common.base.Optional;
import com.splicemachine.derby.iapi.sql.execute.SpliceOperation;
import com.splicemachine.derby.stream.function.SpliceFlatMapFunction;
import com.splicemachine.derby.stream.function.SpliceFunction;
import com.splicemachine.derby.stream.function.SpliceFunction2;
import scala.Tuple2;

import java.util.Comparator;

/**
 * Stream of data acting on a key/values.
 */
public interface PairDataSet<K,V> {
    public DataSet<V> values();
    public DataSet<K> keys();
    public <Op extends SpliceOperation> PairDataSet<K,V> reduceByKey(SpliceFunction2<Op,V,V,V> function2);
    public <Op extends SpliceOperation, U> DataSet<U> map(SpliceFunction<Op,Tuple2<K,V>,U> function);
    public <Op extends SpliceOperation, U> DataSet<U> flatmap(SpliceFlatMapFunction<Op,Tuple2<K,V>,U> function);
    public PairDataSet<K,V> sortByKey(Comparator<K> comparator);
    public <W> PairDataSet<K,Tuple2<V,Optional<W>>> hashLeftOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<Optional<V>,W>> hashRightOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<V,W>> hashJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<V,Optional<W>>> broadcastLeftOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<Optional<V>,W>> broadcastRightOuterJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,Tuple2<V,W>> broadcastJoin(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,V> subtractByKey(PairDataSet<K,W> rightDataSet);
    public <W> PairDataSet<K,V> broadcastSubtractByKey(PairDataSet<K,W> rightDataSet);
    public String toString();

/*    public <W> PairDataSet<K,Tuple2<V,Optional<W>>> nestedLoopLeftOuterJoin(PairDataSet<K,W> rightDataSet); ?
    public <W> PairDataSet<K,Tuple2<Optional<V>,W>> nestedLoopRightOuterJoin(PairDataSet<K,W> rightDataSet); ?
    public <W> PairDataSet<K,Tuple2<V,W>> nestedLoopJoin(PairDataSet<K,W> rightDataSet); ?
*/
}