# DUUI-Gateway

![GitHub License](https://img.shields.io/github/license/Texttechnologylab/DUUI-Gateway)

### About
The Docker Unified UIMA Interface – Gateway (DUUI-Gateway for short) is a web and REST-based software solution for encapsulating and utilising the Docker Unified UIMA Interface, a Big Data NLP framework for the automatic processing of heterogeneous NLP tools, based on UIMA and using microservices such as Docker or Kubernetes.

**DUUI** as well as **DUUI-Gateway** are developed and maintained at the **Texttechnologylab** ([TTLab](https://www.texttechnologylab.org/)) at the Goethe University Frankfurt.


## Introduction

Automatic analysis of large text corpora is a complex task. This complexity particularly concerns the question of time efficiency. Furthermore, efficient, flexible, and extensible textanalysis requires the continuous integration of every new text analysis tools. Since there are currently, in the area of NLP and especially in the application context of UIMA, only very few to no adequate frameworks for these purposes, which are not simultaneously outdated or can no longer be used for security reasons, this work will present a new approach to fill this gap.

## Team

- Cederic Borkowski [:fontawesome-brands-github:](https://github.com/CedricBorko)
- Prof. Dr. Alexander Mehler (Leader TTLab) [:fontawesome-brands-github:](https://github.com/amehler) [:fontawesome-brands-researchgate:](https://www.researchgate.net/profile/Alexander-Mehler-2)
- Giuseppe Abrami [:fontawesome-brands-github:](https://github.com/abrami) [:fontawesome-brands-researchgate:](https://www.researchgate.net/profile/Giuseppe-Abrami)
- Davit Terefe


## Pipeline

A pipeline is a collection of components or Analysis Engines that can be executed. During an analysis process, the components in the pipeline are executed one after
another annotating documents. Pipelines do not interact with the input data directly but build the structure for an NLP workflow.

Creating a pipeline with this web-interface can be done in the Builder. It is a three-step form that guides you through building a pipeline either from scratch or 
using a template as the starting point.

>Choosing a template as a starting point copies all predefined settings into a fresh
pipeline.

In the second step pipeline specific properties like name, description, tags and settings can be edited. 
Only a name is required to proceed but adding a short description is recommended to serve as documentation 
and help others when sharing a pipeline. Tags can help document and find pipelines
in the Dashboard.

## Component

Components are the part of DUUI that actually do the processing and therefore offer
the most settings. When creating a pipeline you can choose from a set of predefined
components or create your own. Once added to the pipeline, a component can be edited
by clicking the <img src="./images/fa-edit.svg" width="14"> icon. This will open a drawer on
the right, that allows for modification of a component.

Settings include:

**Name**

**Driver** &mdash; The Driver is responsible for the instantiation
  of a component during a process.

**Target** &mdash; The component's target depends on the selected
driver. For Docker, Kubernetes and Swarm Drivers, the target is the full image name.
For UIMA it is the class path to the Annotator represented by this component and for
a Remote Driver the URL has to be specified.

**Tags**  

**Description**  

**Options**  

**Parameters**

Options are specific to the selected driver. Most of the time the default options
are sufficient and modifications are only for special uses cases. Parameters are
useful if the component requires settings that are not controlled by DUUI.

>When editing a specific pipeline, clicking the <img src="./images/fa-clone.svg" width="14"> icon
clones the component's settings and prefills the creation form.

## Process 

A process manages the flow of data and pipeline execution. Starting a process is
possible on a pipeline page. On the process creation screen you are asked to select
an input, output and optionally settings that influence the process behavior.

### Input and Output 

Any process must be provided with an input source to be started. Each requires
different properties to be set. The available input sources are:

#### Text

For simple and quick analysis you can choose to process plain text. The text
to be analyzed can be entered in a text area.

#### File 

Selecting file as the input source allows for the upload of one or multiple
files.

#### Cloud 

There are currently four cloud storage providers available to use: Dropbox and
Min.io (s3), Google Drive, and NextCloud. More will be added in the future. To use your cloud storage
provider of choice, a connection must be established on your Account page.

>With the exception of text, all input sources require a file extension to be
selected.

### Settings 

Settings can be changed for both the input and output. Their main purpose is to
filter the files that are processed. This can be done by setting a minimum file
size or ignoring files that may be at the output location.

Process related settings include the option to use multiple workers for parallel
processing or ignoring errors that occur by skipping to next docment instead of
failing the entire pipeline.

Note that the amount of workers or threads that can be used is limited by the
system!
