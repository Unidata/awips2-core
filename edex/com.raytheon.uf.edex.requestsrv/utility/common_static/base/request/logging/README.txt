Request Filtering
=================

Request details can be logged to the edex-request-thriftSrv to allow analysis
of weather product usage across AWIPS by external applications.  Logging is
configured by request class.  All requests are implementations of the 
IServerRequest interface.  Separate configuration files are provided for 
different EDEX instances:
    - request
    - bmh
    - registry
    - centralRegistry

Request logging is configured as a white list: only classes explicitly 
identified and enabled are logged.  By default, all request attributes are 
logged for enabled classes.  The standard localization override strategy is
supported.  Configuration files are located in common_static:request/logging.
Configuration is additive, with the value from the most specific configuration
file taking effect in the event of conflicts.

Request logging can be disabled in one of two ways: 
    1) delete request_logging.xml from all configuration locations; or
    2) set loggingEnabled="false" in the requests element of the most 
       specific instance of the appropriate configuration file.


Sample configuration:

    <?xml version='1.0' encoding='UTF-8'?> 
    <requests loggingEnabled="true" maxFieldStringLength="150" maxJsonStringLength="8192">
        <request class="com.raytheon.uf.common.dataquery.requests.DbQueryRequest"/>
        <request class="com.raytheon.uf.common.dataquery.requests.DbQueryRequestSet"/>
        <request class="com.raytheon.uf.common.dataquery.requests.TimeQueryRequestSet"/>
        <request class="com.raytheon.uf.common.pointdata.PointDataServerRequest"/>
        <request class="com.raytheon.uf.common.dataquery.requests.QlServerRequest">
            <attributes>
                <attribute name="query" maxLength="80"/>
                <attribute name="lang" enabled="false"/>
            </attributes>
        </request>
    </requests>

XML elements
============

requests (required)
--------
Singleton element that defines global (for all requests) attributes and wraps
all defined requests.

    XML attributes
    --------------
    discoveryMode (boolean, default: "false") - Special mode to aid in developing
        configuration files.  Operationally, this will either be omitted from
        the configuration, or it will be set to "false".  In discovery mode, 
        all requests are logged unless explicitly disabled (see the request tag, 
        below).  To enabled, set to "true" and bounce the EDEX instance.  Run 
        for some period of time while exercising the appropriate applications 
        (CAVE, GFE, etc).  Review the log file for this EDEX instance to identify 
        the desired request classes and define them as enabled in a config file.
        To assist with this process, you can explicitly disable classes you aren't 
        interested in logging by setting enabled="false" for the request.  When 
        done with discovery, either remove the discoveryMode attribute from the 
        requests tag, or set it to "false".  You can also remove all request
        elements where the enabled XML attribute is set to "false".

    loggingEnabled (boolean, default: "true") - If "false", then no requests
        are logged.  If "true", only the request types defined in the request
        elements with enabled="true" are logged.

    maxFieldStringLength (int, default: 160) - Maximum length of all string 
        attribute values.  Any string values longer will be truncated and 
        terminated with "...".

    maxJsonStringLength (int, default: 8192) - Maximum length of the JSON log
        string.  All JSON strings longer will be truncated at this value and 
        terminated with "...".

request (optional)
-------
This element describes the logging characteristics for a single request class.
A request element must be defined for each request class to be logged.  Individual
request attributes can be disabled or truncated as well (see the attributes
element, below).

    XML Attributes
    --------------
    className (String) - Fully qualified request class name.

    enabled (boolean, default: "true") - If not present or explicitly set to 
        "true", requests of this type will be logged.  To disable logging of
        this request class, set to "false".

attributes
----------
This element wraps all individual attribute elements for a single request class.
It contains zero or more attribute elements.  The attributes element is only
required for a request if individual attributes are being configured.

attribute
---------
Specification for filtering a single request object attribute.  Two filters
exist: enabled and maxLength.  By default, all class attributes are logged.

    XML Attributes
    --------------
    name (String) - Request object attribute name.  This is case sensitive.

	enabled (boolean, default: "true") - If not present or explicitly set to 
        "true", this attribute will be logged.  Set to "false" to not log this
        attribute for this request class.

	maxLength (int, default: -1) - Determines the maximum length of a string
		attribute.  The default, -1, is to output the full string, up to the
		global maxFieldStringLength value.  Strings will be truncated to 
		maxFieldStringLength regardless of the maxLength value.
