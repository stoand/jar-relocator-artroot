# Java Relocator (Anti Proguard)

Goes through an input jar and moves all classes
in the root package into the `artroot` package.

## Why

If you want to copy and override classes defined in
a jar that was processed with Proguard you will run into the annoyance
that Proguard put classes in the root and these cannot be imported. That's why this
tool moves all classes in the root into a new `artroot` package so they can be referenced.

## Usage

* Download `jar-relocator.jar` from the releases of this project.
* `java -jar jar-relocator.jar input.jar output.jar`