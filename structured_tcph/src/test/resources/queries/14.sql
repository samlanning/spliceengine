-- $ID$
-- TPC-H/TPC-R Promotion Effect Query (Q14)
-- Functional Query Definition
-- Approved February 1998
set schema tpch1x;
VALUES (CURRENT_TIMESTAMP);

select
	100.00 * sum(case
		when p_type like 'PROMO%'
			then l_extendedprice * (1 - l_discount)
		else 0
	end) / sum(l_extendedprice * (1 - l_discount)) as promo_revenue
from
	lineitem,
	part
where
        l_partkey = p_partkey
        and l_shipdate >= date('1995-09-01')
--      and l_shipdate < date '1995-09-01' + interval '1' month;
        and l_shipdate < date({fn TIMESTAMPADD(SQL_TSI_MONTH, 1, cast('1995-09-01 00:00:00' as timestamp))}) ;
VALUES (CURRENT_TIMESTAMP);
