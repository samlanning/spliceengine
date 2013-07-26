create schema tpcd1x;
set scehema tpcd1x;
-- Sccsid:     @(#)dss.ddl	2.1.8.1
CREATE TABLE LINEITEM ( L_ORDERKEY    INTEGER NOT NULL PRIMARY KEY,
                             L_PARTKEY     INTEGER NOT NULL,
                             L_SUPPKEY     INTEGER NOT NULL,
                             L_LINENUMBER  INTEGER NOT NULL,
                             L_QUANTITY    DECIMAL(15,2) NOT NULL,
                             L_EXTENDEDPRICE  DECIMAL(15,2) NOT NULL,
                             L_DISCOUNT    DECIMAL(15,2) NOT NULL,
                             L_TAX         DECIMAL(15,2) NOT NULL,
                             L_RETURNFLAG  CHAR(1) NOT NULL,
                             L_LINESTATUS  CHAR(1) NOT NULL,
                             L_SHIPDATE    DATE NOT NULL,
                             L_COMMITDATE  DATE NOT NULL,
                             L_RECEIPTDATE DATE NOT NULL,
                             L_SHIPINSTRUCT CHAR(25) NOT NULL,
                             L_SHIPMODE     CHAR(10) NOT NULL,
                             L_COMMENT      VARCHAR(44) NOT NULL,
                      CONSTRAINT LI_UNIQ UNIQUE(L_PARTKEY,L_SUPPKEY)
                      );
--ALTER TABLE LINEITEM ADD CONSTRAINT LI_UNIQ UNIQUE(L_PARTKEY,L_SUPPKEY);

CREATE TABLE ORDERS  ( O_ORDERKEY       INTEGER NOT NULL PRIMARY KEY,
                           O_CUSTKEY        INTEGER NOT NULL UNIQUE,
                           O_ORDERSTATUS    CHAR(1) NOT NULL,
                           O_TOTALPRICE     DECIMAL(15,2) NOT NULL,
                           O_ORDERDATE      DATE NOT NULL,
                           O_ORDERPRIORITY  CHAR(15) NOT NULL,
                           O_CLERK          CHAR(15) NOT NULL,
                           O_SHIPPRIORITY   INTEGER NOT NULL,
                           O_COMMENT        VARCHAR(79) NOT NULL,
--                    ):
             FOREIGN KEY(O_ORDERKEY) REFERENCES LINEITEM(L_ORDERKEY));

CREATE TABLE CUSTOMER ( C_CUSTKEY     INTEGER NOT NULL PRIMARY KEY,
                             C_NAME        VARCHAR(25) NOT NULL,
                             C_ADDRESS     VARCHAR(40) NOT NULL,
                             C_NATIONKEY   INTEGER NOT NULL UNIQUE,
                             C_PHONE       CHAR(15) NOT NULL,
                             C_ACCTBAL     DECIMAL(15,2)   NOT NULL,
                             C_MKTSEGMENT  CHAR(10) NOT NULL,
                             C_COMMENT     VARCHAR(117) NOT NULL,
--                      );
           FOREIGN KEY(C_CUSTKEY) REFERENCES ORDERS(O_CUSTKEY));

CREATE TABLE PARTSUPP ( PS_PARTKEY     INTEGER NOT NULL UNIQUE,
                             PS_SUPPKEY     INTEGER NOT NULL UNIQUE,
                             PS_AVAILQTY    INTEGER NOT NULL ,
                             PS_SUPPLYCOST  DECIMAL(15,2)  NOT NULL,
                             PS_COMMENT     VARCHAR(199) NOT NULL,
                        PRIMARY KEY(PS_PARTKEY,PS_SUPPKEY),
--                      );
           FOREIGN KEY(PS_PARTKEY,PS_SUPPKEY) REFERENCES LINEITEM(L_PARTKEY,L_SUPPKEY));

CREATE TABLE SUPPLIER ( S_SUPPKEY     INTEGER NOT NULL PRIMARY KEY,
--                             S_NAME        CHAR(25) NOT NULL,
                             S_NAME        VARCHAR(25) NOT NULL,
                             S_ADDRESS     VARCHAR(40) NOT NULL,
                             S_NATIONKEY   INTEGER NOT NULL UNIQUE,
                             S_PHONE       CHAR(15) NOT NULL,
                             S_ACCTBAL     DECIMAL(15,2) NOT NULL,
                             S_COMMENT     VARCHAR(101) NOT NULL,
--                      );
             FOREIGN KEY (S_SUPPKEY) REFERENCES  PARTSUPP(PS_SUPPKEY));

CREATE TABLE PART  ( P_PARTKEY     INTEGER NOT NULL PRIMARY KEY,
                          P_NAME        VARCHAR(55) NOT NULL,
                          P_MFGR        CHAR(25) NOT NULL,
                          P_BRAND       CHAR(10) NOT NULL,
                          P_TYPE        VARCHAR(25) NOT NULL,
                          P_SIZE        INTEGER NOT NULL,
                          P_CONTAINER   CHAR(10) NOT NULL,
                          P_RETAILPRICE DECIMAL(15,2) NOT NULL,
                          P_COMMENT     VARCHAR(23) NOT NULL,
--                   );
             FOREIGN KEY (P_PARTKEY) REFERENCES PARTSUPP(PS_PARTKEY));

CREATE TABLE REGION  ( R_REGIONKEY  INTEGER NOT NULL,
--                            R_NAME       CHAR(25) NOT NULL,
                            R_NAME       VARCHAR(25) NOT NULL PRIMARY KEY,
                            R_COMMENT    VARCHAR(152));
--            FOREIGN KEY(R_REGIONKEY) references NATION(N_REGIONKEY);

CREATE TABLE NATION  ( N_NATIONKEY  INTEGER NOT NULL,
--                            N_NAME       CHAR(25) NOT NULL,
                            N_NAME       VARCHAR(25) NOT NULL,
                            N_REGIONKEY  INTEGER NOT NULL,
                            N_COMMENT    VARCHAR(152),
                            primary key (N_NATIONKEY),
--                     );
            FOREIGN KEY(N_NATIONKEY) REFERENCES CUSTOMER(C_NATIONKEY),
            FOREIGN KEY(N_NATIONKEY) REFERENCES SUPPLIER(S_NATIONKEY)); 
                    


SELECT CONGLOMERATENAME FROM SYS.SYSCONGLOMERATES,
SYS.SYSCONSTRAINTS WHERE
SYS.SYSCONGLOMERATES.TABLEID = SYS.SYSCONSTRAINTS.TABLEID
AND CONSTRAINTNAME = 'LI_UNIQ';

EXIT;
