Neo4j nodes still in index after deletion
=========================================

Test case for the following Neo4j regression in 4.3.x.

To reproduce:

1. Create a unique label index on a property key
2. Create a standard index on a second property
3. In a single transaction:
  1. Create a node with the index label
  2. Add properties for each index
  3. Delete the node
  4. Check the node is gone from both indexes

## What should happen:

 - The node should be gone from both indexes after deletion

## What does happen in Neo4j >= 4.3.0

 - The node is gone from the standard index
 - The node is found in the unique index
 - Accessing data results in an `EntityNotFoundException`

## To run passing tests in Neo4j 4.2.9

    mvn test -Dneo4j.version=4.2.9 # pass

## To run failing tests in Neo4j 4.3.6

    mvn test -Dneo4j.version=4.3.6


