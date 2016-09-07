# HZSK-CLARIN-Services

This is collection of web services by Hamburger Zentrum für Sprachkorpora for
mainly CLARIN use but can be usable for other interoperability.

## Dependencies

Ideally maven commands will automagicate these:

* Java
* Maven
* Tomcat
* Exmaralda 1.10 (see later)

## Usage

I currently work with it by doing things like:
```
mvn compile
mvn package
mvn install
cp target/*.jar $MAVENDEPLOY/
```

I guess you could also set up the mvn deploy target somehow.

You need to query the web address with parameters operation, version and query.

## EXMaRALDA dependency

Exmaralda doesn't do maven so well. You can work it by donwloading the package
you need and executing something like:

    tar zxvf ~/Downloads/prev_EXMARaLDA_SUN_JMF.tar.gz
    mvn install:install-file -Dfile=lib/EXMARaLDA.jar -DgroupId=org.exmaralda -DartifactId=exmaralda -Dversion=1.10 -Dpackaging=jar

(I found these instructions by googling from:
http://www.mkyong.com/maven/how-to-include-library-manully-into-maven-local-repository/)

