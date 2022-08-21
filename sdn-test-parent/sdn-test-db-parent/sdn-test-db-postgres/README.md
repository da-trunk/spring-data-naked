# starting the container

	* `docker run -it --rm --name postgres -p 5432:5432 -e POSTGRES_PASSWORD=password -e POSTGRES_HOST_AUTH_METHOD=trust -e POSTGRES_USER=test -e POSTGRES_DB=test postgres:14.2`
	* `docker exec -it postgres psql -U test -W -d test`
	

# testing from host
	
	* `sudo apt install -y postgresql-client`
	* `pg_isready -d dbname -h localhost -p 5432 -U postgres`
	* `pg_isready -d test -h localhost -p 5432 -U test`
	* `sudo apt install postgresql postgresql-contrib`
		* `psql -h localhost -U test -W -d test`