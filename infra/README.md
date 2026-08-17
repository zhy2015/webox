# Infrastructure

Infrastructure definitions belong here. The first implementation should provide Docker Compose for a standalone MySQL service and persistent local volumes. Optional services, such as Redis or an object store, must be introduced only with a documented requirement and a health check.

Application source does not belong in this directory. Secrets must be supplied through ignored environment files or the deployment platform, never committed.
