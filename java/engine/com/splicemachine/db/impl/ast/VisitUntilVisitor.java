package com.splicemachine.db.impl.ast;

import com.google.common.base.Predicate;
import com.splicemachine.db.iapi.error.StandardException;
import com.splicemachine.db.iapi.sql.compile.Visitable;
import com.splicemachine.db.iapi.sql.compile.Visitor;

/**
 * Visitor that applies a visitor until a stopping point defined by a predicate. Parameters of the traversal
 * defined by the wrapped visitor.
 *
 * @author P Trolard
 *         Date: 30/10/2013
 */
public class VisitUntilVisitor implements Visitor {
    private boolean stop = false;
    final Visitor v;
    final Predicate<? super Visitable> pred;

    public VisitUntilVisitor(final Visitor v, final Predicate<? super Visitable> pred){
        this.v = v;
        this.pred = pred;
    }

    @Override
    public Visitable visit(Visitable node) throws StandardException {
        if (pred.apply(node)){
            stop = true;
            return node;
        }
        return v.visit(node);
    }

    @Override
    public boolean visitChildrenFirst(Visitable node) {
        return v.visitChildrenFirst(node);
    }

    @Override
    public boolean stopTraversal() {
        return stop;
    }

    @Override
    public boolean skipChildren(Visitable node) throws StandardException {
        return v.skipChildren(node);
    }
}
