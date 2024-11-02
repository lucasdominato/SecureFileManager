GET File by ID including username into the query to avoid unnecessary queries in the database
This might be less flexible for future changes but increase the performance because there is a index for username

Chose to use structured fields for metadata instead of key-value to avoid complex queries and type validation
