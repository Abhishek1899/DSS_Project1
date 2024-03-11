

### Skiresorts

In order to run this project, make sure to have
1. Java
2. Maven
3. VSCode



#### Maven Build

``` bash 
mvn clean install -Dmaven.test.skip=true
```

> The server config for development is at `pom.xml:86`.
>
> Feel free to configure it.


#### Maven Execute.

``` bash
mvn exec:exec
```
> The server config for deployment is in `org.example.Skiresorts`.


Then you can access the servlet by:

http://localhost:8080/Skiresorts/Skiers

#### Testing

``` bash
mvn test
```


