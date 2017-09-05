.. This work is licensed under a Creative Commons Attribution 4.0 International License.
.. http://creativecommons.org/licenses/by/4.0
.. Copyright © 2017 AT&T Intellectual Property. All rights reserved.

Component Information Template
==============================
.. High level architecture, design, and packaging information for release planning and delivery.

CLAMP is a platform for designing and managing control loops.

It is used to design a closed loop, configure it with specific parameters for a particular network service, then deploying and undeploying it.
Once deployed, the user can also update the loop with new parameters during runtime, as well as suspending and restarting it.
It interacts with other systems to deploy and execute the closed loop. For example, it pushes the control loop design to the SDC catalog, associating it with the VF resource. It requests from DCAE the instantiation of microservices to manage the closed loop flow. Further, it creates and updates multiple policies in the Policy Engine that define the closed loop flow.
The CLAMP platform abstracts the details of these systems under the concept of a control loop model. The design of a control loop and its management is represented by a workflow in which all relevant system interactions take place.

CLAMP is delivered as a single container


.. toctree::
:maxdepth: 1


Delivery
--------
CLAMP component is composed of a UI layer and a BackEND layer and packaged into a single container as illustrated in the following diagrams.

.. blockdiag::


   blockdiag layers {
   orientation = portrait
   a -> m;
   b -> n;
   c -> x;
   m -> y;
   m -> z;
   group l1 {
	color = blue;
	x; y; z;
	}
   group l2 {
	color = yellow;
	m; n;
	}
   group l3 {
	color = orange;
	a; b; c;
	}

   }


Logging & Diagnostic Information
--------------------------------
Description of how to interact with and diagnose problems with the components in the run-time packaging.


Installation
------------
Steps to Install


Configuration
-------------
Where are they provided?
What are parameters and values?


Administration
--------------

How to run and manage the component.


Human Interfaces
----------------
Basic info on the interface type, ports/protocols provided over, etc.