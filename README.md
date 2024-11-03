GET File by ID including username into the query to avoid unnecessary queries in the database
This might be less flexible for future changes but increase the performance because there is a index for username

Chose to use structured fields for metadata instead of key-value to avoid complex queries and type validation

I've considered to use an external storage such as Amazon S3 to store the file binaries, but the requirements asked to persiste data in database.

To use database to store file binaries my suggestion would be to use separate metadata from binaries in two different tables.
Using this strategy we can improve index and performance of the metadata.

For the binaries we can use a couple of techniques such as TOAST, partitioning and even multi disk for binaries tablespace (improving I/O concurrency for data-only tables and binaries table).
