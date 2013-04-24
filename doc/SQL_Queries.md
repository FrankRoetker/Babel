# SQL Server -> Neo4j



The goal of this guide is to familiarize our audience with
the process in which Babel is going to tackle the Microsoft
SQL Server to Neo4j conversion problem. 


## Selecting all of the Tables


Selecting all of the tables is easy:

```SQL
SELECT name
FROM sys.Tables
```

However this won't exactly cut it for what we are trying
to do with the database. We are going to need more information!


## Selecting all of the Tables and their Columns


How about getting all of the column names to go along with
all of the tables?

```SQL
SELECT TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.COLUMNS
ORDER BY TABLE_NAME, ORDINAL_POSITION
```

Now we're talking! We are getting closer to having enough information
to define our schema for our nodes.


## Selecting Foreign Key Info


One of the things we haven't gone over yet is relationships. How
are we going to define relationships between nodes? 

In a relational database, foreign key tables are commonly used to join
multiple tables together. This is the equivalent to relationships in
our graph database.

So lets get some foreign keys, their tables, and the tables/columns that
they are constrained to:

```SQL
SELECT C.TABLE_NAME [PKTABLE_NAME], 
       KCU.COLUMN_NAME [PKCOLUMN_NAME],
       C2.TABLE_NAME [FKTABLE_NAME], 
       KCU2.COLUMN_NAME [FKCOLUMN_NAME]
FROM   INFORMATION_SCHEMA.TABLE_CONSTRAINTS C 
       INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU 
         ON C.CONSTRAINT_SCHEMA = KCU.CONSTRAINT_SCHEMA 
            AND C.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME 
       INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC 
         ON C.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA 
            AND C.CONSTRAINT_NAME = RC.CONSTRAINT_NAME 
       INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS C2 
         ON RC.UNIQUE_CONSTRAINT_SCHEMA = C2.CONSTRAINT_SCHEMA 
            AND RC.UNIQUE_CONSTRAINT_NAME = C2.CONSTRAINT_NAME 
       INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU2 
         ON C2.CONSTRAINT_SCHEMA = KCU2.CONSTRAINT_SCHEMA 
            AND C2.CONSTRAINT_NAME = KCU2.CONSTRAINT_NAME 
            AND KCU.ORDINAL_POSITION = KCU2.ORDINAL_POSITION 
WHERE  C.CONSTRAINT_TYPE = 'FOREIGN KEY'
```



## Merging the Two


We can actually merge the last two queries:

```SQL
SELECT INFO.TABLE_NAME, INFO.COLUMN_NAME, 
       FK.FKTABLE_NAME, FK.FKCOLUMN_NAME 
FROM

(SELECT TABLE_NAME, COLUMN_NAME
        FROM INFORMATION_SCHEMA.COLUMNS) AS INFO

LEFT OUTER JOIN

(
  SELECT C.TABLE_NAME [TABLE_NAME], 
         KCU.COLUMN_NAME [COLUMN_NAME],
         C2.TABLE_NAME [FKTABLE_NAME], 
         KCU2.COLUMN_NAME [FKCOLUMN_NAME]
  FROM   INFORMATION_SCHEMA.TABLE_CONSTRAINTS C 
         INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU 
           ON C.CONSTRAINT_SCHEMA = KCU.CONSTRAINT_SCHEMA 
              AND C.CONSTRAINT_NAME = KCU.CONSTRAINT_NAME 
         INNER JOIN INFORMATION_SCHEMA.REFERENTIAL_CONSTRAINTS RC 
           ON C.CONSTRAINT_SCHEMA = RC.CONSTRAINT_SCHEMA 
              AND C.CONSTRAINT_NAME = RC.CONSTRAINT_NAME 
         INNER JOIN INFORMATION_SCHEMA.TABLE_CONSTRAINTS C2 
           ON RC.UNIQUE_CONSTRAINT_SCHEMA = C2.CONSTRAINT_SCHEMA 
              AND RC.UNIQUE_CONSTRAINT_NAME = C2.CONSTRAINT_NAME 
         INNER JOIN INFORMATION_SCHEMA.KEY_COLUMN_USAGE KCU2 
           ON C2.CONSTRAINT_SCHEMA = KCU2.CONSTRAINT_SCHEMA 
              AND C2.CONSTRAINT_NAME = KCU2.CONSTRAINT_NAME 
              AND KCU.ORDINAL_POSITION = KCU2.ORDINAL_POSITION 
  WHERE  C.CONSTRAINT_TYPE = 'FOREIGN KEY'
) AS FK

ON FK.TABLE_NAME = INFO.TABLE_NAME
   AND FK.COLUMN_NAME = INFO.COLUMN_NAME
ORDER BY INFO.TABLE_NAME, INFO.COLUMN_NAME
```

This consolidation of data is what we are going to need to consider when
designing the algorithm for distinguishing potential nodes from relationships.

## Writing Nodes

Now that we can decide what Tables are going to be made into nodes,
we should start defining the process on how that should happen.

Creating nodes in Cypher is actually 
[really simple](http://docs.neo4j.org/chunked/milestone/query-create.html). 
Lets show an example to drive the point home:

```Cypher
CREATE n = {name : 'Frank', title : 'Developer'}
```


## Writing Relationships

Creating relationships in Cypher is also 
[simple](http://docs.neo4j.org/chunked/milestone/query-create.html).

```Cypher
START a=node(1), b=node(2)
CREATE a<-[r:FRIENDS {name : a.name + '<->' + b.name }]->b
RETURN r
```


## Moving on to Primary Keys


```SQL
SELECT tc.TABLE_NAME, COLUMN_NAME
FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS tc
JOIN INFORMATION_SCHEMA.CONSTRAINT_COLUMN_USAGE ccu 
ON tc.CONSTRAINT_NAME = ccu.Constraint_name
WHERE tc.CONSTRAINT_TYPE = 'Primary Key'
```


## Creating indexes in Cypher

As stated in [The Neo4j Manual](http://docs.neo4j.org/chunked/milestone/indexing-create.html):

> An index is created if it doesn’t exist when you ask for it. 
> Unless you give it a custom configuration, it will be created 
> with default configuration and backend.

```Cypher
CREATE INDEX ON :Person(name);
```

We can also put [labels](http://blog.neo4j.org/2013/04/nodes-are-people-too.html)
onto the nodes for giving them their type and index. In this case, we would be
giving nodes a label based on their table name.

```Cypher
CREATE (n:Person = {name : 'Frank', title : 'Developer'})
RETURN n;
```

