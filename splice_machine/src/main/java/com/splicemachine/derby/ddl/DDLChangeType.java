package com.splicemachine.derby.ddl;

/**
 * Indicate the type of DDL change.  These are currently largely unused.  Maybe eventually consolidate with
 * action constants at top of derby's DependencyManager interface.
 */
public enum DDLChangeType {

    CHANGE_PK(true),
    ADD_CHECK(true),
    ADD_FOREIGN_KEY(true),
    CREATE_INDEX(true),
    ADD_NOT_NULL(true),
    ADD_COLUMN(true),
    ADD_PRIMARY_KEY(true),
    ADD_UNIQUE_CONSTRAINT(true),
    DROP_COLUMN(true),
    DROP_CONSTRAINT(true),
    DROP_PRIMARY_KEY(true),
    DROP_TABLE(true),
    DROP_SCHEMA(true),
    DROP_INDEX(true),
    DROP_FOREIGN_KEY(true),
    ENTER_RESTORE_MODE(false);

    private boolean tentative;

    DDLChangeType(boolean tentative) {
        this.tentative = tentative;
    }

    public boolean isTentative() {
        return tentative;
    }

    @Override
    public String toString() {
        return super.toString() + "{" + "tentative=" + tentative + '}';
    }
}
