# Document Page Counter
A handy app that counts all pages in known document types found in a directory and its subdirs

The application traverses all files and directories in the target directory and outputs each document found, and the number of pages in it.
At the end the total number of documents and the total number of pages are printed.

At the moment the application supports pdf and docx document. Other types can be added on request.

## Usage
Execute the following in the directory where the jar is located (or add the jar path to java classpath)

`java -jar DocumentPageCounter-0.2-full.jar <target directory>`

## Building

Execute in the project root directory:

`mvn clean install`

After the build the executable jar shall be found in the `target` directory.

## Requirements

Requirements for building: java8, maven 3

Requirements for running: java 8/jre 8
