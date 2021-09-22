;(function(DocsAPI, window, document, undefined) {

    DocsAPI.DocEditor = function(placeholderId, config) {
        var _self = this,
            _config = config || {};

        extend(_config, DocsAPI.DocEditor.defaultConfig);
        _config.editorConfig.canUseHistory = _config.events && !!_config.events.onRequestHistory;
        _config.editorConfig.canHistoryClose = _config.events && !!_config.events.onRequestHistoryClose;
        _config.editorConfig.canHistoryRestore = _config.events && !!_config.events.onRequestRestore;
        _config.editorConfig.canSendEmailAddresses = _config.events && !!_config.events.onRequestEmailAddresses;
        _config.editorConfig.canRequestEditRights = _config.events && !!_config.events.onRequestEditRights;
        _config.editorConfig.canRequestClose = _config.events && !!_config.events.onRequestClose;
        _config.editorConfig.canRename = _config.events && !!_config.events.onRequestRename;
        _config.editorConfig.canMakeActionLink = _config.events && !!_config.events.onMakeActionLink;
        _config.editorConfig.canRequestUsers = _config.events && !!_config.events.onRequestUsers;
        _config.editorConfig.canRequestSendNotify = _config.events && !!_config.events.onRequestSendNotify;
        _config.editorConfig.mergeFolderUrl = _config.editorConfig.mergeFolderUrl || _config.editorConfig.saveAsUrl;
        _config.editorConfig.canRequestSaveAs = _config.events && !!_config.events.onRequestSaveAs;
        _config.editorConfig.canRequestInsertImage = _config.events && !!_config.events.onRequestInsertImage;
        _config.editorConfig.canRequestMailMergeRecipients = _config.events && !!_config.events.onRequestMailMergeRecipients;
        _config.editorConfig.canRequestCompareFile = _config.events && !!_config.events.onRequestCompareFile;
        _config.editorConfig.canRequestSharingSettings = _config.events && !!_config.events.onRequestSharingSettings;
        _config.editorConfig.canRequestCreateNew = _config.events && !!_config.events.onRequestCreateNew;
        _config.frameEditorId = placeholderId;
        _config.parentOrigin = window.location.origin;

        var onMouseUp = function (evt) {
            _processMouse(evt);
        };

        var _attachMouseEvents = function() {
            if (window.addEventListener) {
                window.addEventListener("mouseup", onMouseUp, false)
            } else if (window.attachEvent) {
                window.attachEvent("onmouseup", onMouseUp);
            }
        };

        var _detachMouseEvents = function() {
            if (window.removeEventListener) {
                window.removeEventListener("mouseup", onMouseUp, false)
            } else if (window.detachEvent) {
                window.detachEvent("onmouseup", onMouseUp);
            }
        };

        var _onAppReady = function() {
            if (_config.type === 'mobile') {
                document.body.onfocus = function(e) {
                    setTimeout(function(){
                        iframe.contentWindow.focus();

                        _sendCommand({
                            command: 'resetFocus',
                            data: {}
                        })
                    }, 10);
                };
            }

            _attachMouseEvents();

            if (_config.editorConfig) {
                _init(_config.editorConfig);
            }

            if (_config.document) {
                _openDocument(_config.document);
            }
        };

        var _callLocalStorage = function(data) {
            if (data.cmd == 'get') {
                if (data.keys && data.keys.length) {
                    var af = data.keys.split(','), re = af[0];
                    for (i = 0; ++i < af.length;)
                        re += '|' + af[i];

                    re = new RegExp(re); k = {};
                    for (i in localStorage)
                        if (re.test(i)) k[i] = localStorage[i];
                } else {
                    k = localStorage;
                }

                _sendCommand({
                    command: 'internalCommand',
                    data: {
                        type: 'localstorage',
                        keys: k
                    }
                });
            } else
            if (data.cmd == 'set') {
                var k = data.keys, i;
                for (i in k) {
                    localStorage.setItem(i, k[i]);
                }
            }
        };

        var _onMessage = function(msg) {
            if ( msg ) {
                if ( msg.type === "onExternalPluginMessage" ) {
                    _sendCommand(msg);
                } else
                if ( msg.frameEditorId == placeholderId ) {
                    var events = _config.events || {},
                        handler = events[msg.event],
                        res;

                    if (msg.event === 'onRequestEditRights' && !handler) {
                        _applyEditRights(false, 'handler isn\'t defined');
                    } else if (msg.event === 'onInternalMessage' && msg.data && msg.data.type == 'localstorage') {
                        _callLocalStorage(msg.data.data);
                    } else {
                        if (msg.event === 'onAppReady') {
                            _onAppReady();
                        }

                        if (handler && typeof handler == "function") {
                            res = handler.call(_self, {target: _self, data: msg.data});
                        }
                    }
                }
            }
        };

        var _checkConfigParams = function() {
            if (_config.document) {
                if (!_config.document.url || ((typeof _config.document.fileType !== 'string' || _config.document.fileType=='') &&
                                              (typeof _config.documentType !== 'string' || _config.documentType==''))) {
                    window.alert("One or more required parameter for the config object is not set");
                    return false;
                }

                var appMap = {
                        'text': 'docx',
                        'text-pdf': 'pdf',
                        'spreadsheet': 'xlsx',
                        'presentation': 'pptx'
                    }, app;

                if (typeof _config.documentType === 'string' && _config.documentType != '') {
                    app = appMap[_config.documentType.toLowerCase()];
                    if (!app) {
                        window.alert("The \"documentType\" parameter for the config object is invalid. Please correct it.");
                        return false;
                    } else if (typeof _config.document.fileType !== 'string' || _config.document.fileType == '') {
                        _config.document.fileType = app;
                    }
                }

                if (typeof _config.document.fileType === 'string' && _config.document.fileType != '') {
                    _config.document.fileType = _config.document.fileType.toLowerCase();
                    var type = /^(?:(xls|xlsx|ods|csv|xlst|xlsy|gsheet|xlsm|xlt|xltm|xltx|fods|ots)|(pps|ppsx|ppt|pptx|odp|pptt|ppty|gslides|pot|potm|potx|ppsm|pptm|fodp|otp)|(doc|docx|doct|odt|gdoc|txt|rtf|pdf|mht|htm|html|epub|djvu|xps|docm|dot|dotm|dotx|fodt|ott))$/
                                    .exec(_config.document.fileType);
                    if (!type) {
                        window.alert("The \"document.fileType\" parameter for the config object is invalid. Please correct it.");
                        return false;
                    } else if (typeof _config.documentType !== 'string' || _config.documentType == ''){
                        if (typeof type[1] === 'string') _config.documentType = 'spreadsheet'; else
                        if (typeof type[2] === 'string') _config.documentType = 'presentation'; else
                        if (typeof type[3] === 'string') _config.documentType = 'text';
                    }
                }

                var type = /^(?:(pdf|djvu|xps))$/.exec(_config.document.fileType);
                if (type && typeof type[1] === 'string') {
                    _config.editorConfig.canUseHistory = false;
                }

                if (!_config.document.title || _config.document.title=='')
                    _config.document.title = 'Unnamed.' + _config.document.fileType;

                if (!_config.document.key) {
                    _config.document.key = 'xxxxxxxxxxxxxxxxxxxx'.replace(/[x]/g, function (c) {var r = Math.random() * 16 | 0; return r.toString(16);});
                } else if (typeof _config.document.key !== 'string') {
                    window.alert("The \"document.key\" parameter for the config object must be string. Please correct it.");
                    return false;
                }

                _config.document.token = _config.token;
            }
            
            return true;
        };

        (function() {
            var result = /[\?\&]placement=(\w+)&?/.exec(window.location.search);
            if (!!result && result.length) {
                if (result[1] == 'desktop') {
                    _config.editorConfig.targetApp = result[1];
                    // _config.editorConfig.canBackToFolder = false;
                    if (!_config.editorConfig.customization) _config.editorConfig.customization = {};
                    _config.editorConfig.customization.about = false;
                    _config.editorConfig.customization.compactHeader = false;
                }
            }
        })();

        var target = document.getElementById(placeholderId),
            iframe;

        if (target && _checkConfigParams()) {
            iframe = createIframe(_config);
            if (iframe.src) {
                var pathArray = iframe.src.split('/');
                this.frameOrigin = pathArray[0] + '//' + pathArray[2];
            }
            target.parentNode && target.parentNode.replaceChild(iframe, target);
            var _msgDispatcher = new MessageDispatcher(_onMessage, this);
        }

        /*
         cmd = {
         command: 'commandName',
         data: <command specific data>
         }
         */

        var _destroyEditor = function(cmd) {
            var target = document.createElement("div");
            target.setAttribute('id', placeholderId);

            if (iframe) {
                _msgDispatcher && _msgDispatcher.unbindEvents();
                _detachMouseEvents();
                iframe.parentNode && iframe.parentNode.replaceChild(target, iframe);
            }
        };

        var _sendCommand = function(cmd) {
            if (iframe && iframe.contentWindow)
                postMessage(iframe.contentWindow, cmd);
        };

        var _init = function(editorConfig) {
            _sendCommand({
                command: 'init',
                data: {
                    config: editorConfig
                }
            });
        };

        var _openDocument = function(doc) {
            _sendCommand({
                command: 'openDocument',
                data: {
                    doc: doc
                }
            });
        };

        var _showMessage = function(title, msg) {
            msg = msg || title;
            _sendCommand({
                command: 'showMessage',
                data: {
                    msg: msg
                }
            });
        };

        var _applyEditRights = function(allowed, message) {
            _sendCommand({
                command: 'applyEditRights',
                data: {
                    allowed: allowed,
                    message: message
                }
            });
        };

        var _processSaveResult = function(result, message) {
            _sendCommand({
                command: 'processSaveResult',
                data: {
                    result: result,
                    message: message
                }
            });
        };

        // TODO: remove processRightsChange, use denyEditingRights
        var _processRightsChange = function(enabled, message) {
            _sendCommand({
                command: 'processRightsChange',
                data: {
                    enabled: enabled,
                    message: message
                }
            });
        };

        var _denyEditingRights = function(message) {
            _sendCommand({
                command: 'processRightsChange',
                data: {
                    enabled: false,
                    message: message
                }
            });
        };

        var _refreshHistory = function(data, message) {
            _sendCommand({
                command: 'refreshHistory',
                data: {
                    data: data,
                    message: message
                }
            });
        };

        var _setHistoryData = function(data, message) {
            _sendCommand({
                command: 'setHistoryData',
                data: {
                    data: data,
                    message: message
                }
            });
        };

        var _setEmailAddresses = function(data) {
            _sendCommand({
                command: 'setEmailAddresses',
                data: {
                    data: data
                }
            });
        };

        var _setActionLink = function (data) {
            _sendCommand({
                command: 'setActionLink',
                data: {
                    url: data
                }
            });
        };

        var _processMailMerge = function(enabled, message) {
            _sendCommand({
                command: 'processMailMerge',
                data: {
                    enabled: enabled,
                    message: message
                }
            });
        };

        var _downloadAs = function(data) {
            _sendCommand({
                command: 'downloadAs',
                data: data
            });
        };

        var _setUsers = function(data) {
            _sendCommand({
                command: 'setUsers',
                data: data
            });
        };

        var _showSharingSettings = function(data) {
            _sendCommand({
                command: 'showSharingSettings',
                data: data
            });
        };

        var _setSharingSettings = function(data) {
            _sendCommand({
                command: 'setSharingSettings',
                data: data
            });
        };

        var _insertImage = function(data) {
            _sendCommand({
                command: 'insertImage',
                data: data
            });
        };

        var _setMailMergeRecipients = function(data) {
            _sendCommand({
                command: 'setMailMergeRecipients',
                data: data
            });
        };

        var _setRevisedFile = function(data) {
            _sendCommand({
                command: 'setRevisedFile',
                data: data
            });
        };

        var _processMouse = function(evt) {
            var r = iframe.getBoundingClientRect();
            var data = {
                type: evt.type,
                x: evt.x - r.left,
                y: evt.y - r.top,
                event: evt
            };

            _sendCommand({
                command: 'processMouse',
                data: data
            });
        };

        var _serviceCommand = function(command, data) {
            _sendCommand({
                command: 'internalCommand',
                data: {
                    command: command,
                    data: data
                }
            });
        };

        return {
            showMessage         : _showMessage,
            processSaveResult   : _processSaveResult,
            processRightsChange : _processRightsChange,
            denyEditingRights   : _denyEditingRights,
            refreshHistory      : _refreshHistory,
            setHistoryData      : _setHistoryData,
            setEmailAddresses   : _setEmailAddresses,
            setActionLink       : _setActionLink,
            processMailMerge    : _processMailMerge,
            downloadAs          : _downloadAs,
            serviceCommand      : _serviceCommand,
            attachMouseEvents   : _attachMouseEvents,
            detachMouseEvents   : _detachMouseEvents,
            destroyEditor       : _destroyEditor,
            setUsers            : _setUsers,
            showSharingSettings : _showSharingSettings,
            setSharingSettings  : _setSharingSettings,
            insertImage         : _insertImage,
            setMailMergeRecipients: _setMailMergeRecipients,
            setRevisedFile      : _setRevisedFile
        }
    };


    DocsAPI.DocEditor.defaultConfig = {
        type: 'desktop',
        width: '100%',
        height: '100%',
        editorConfig: {
            lang: 'zh',
            canCoAuthoring: true,
            customization: {
                about: false,
                feedback: false
            }
        }
    };

    DocsAPI.DocEditor.version = function() {
        return '1.0.0';
    };

    MessageDispatcher = function(fn, scope) {
        var _fn     = fn,
            _scope  = scope || window,
            eventFn = function(msg) {
                _onMessage(msg);
            };

        var _bindEvents = function() {
            if (window.addEventListener) {
                window.addEventListener("message", eventFn, false)
            }
            else if (window.attachEvent) {
                window.attachEvent("onmessage", eventFn);
            }
        };

        var _unbindEvents = function() {
            if (window.removeEventListener) {
                window.removeEventListener("message", eventFn, false)
            }
            else if (window.detachEvent) {
                window.detachEvent("onmessage", eventFn);
            }
        };

        var _onMessage = function(msg) {
            if (msg && window.JSON && _scope.frameOrigin==msg.origin ) {

                try {
                    var msg = window.JSON.parse(msg.data);
                    if (_fn) {
                        _fn.call(_scope, msg);
                    }
                } catch(e) {}
            }
        };

        _bindEvents.call(this);

        return {
            unbindEvents: _unbindEvents
        }
    };

    function getBasePath() {
        /*var scripts = document.getElementsByTagName('script'),
            match;

        for (var i = scripts.length - 1; i >= 0; i--) {
            match = scripts[i].src.match(/(.*)api\/documents\/api.js/i);
            if (match) {
                return match[1];
            }
        }*/
        return "http://"+location.hostname+":9000/web-apps/apps/";
    }

    function getExtensionPath() {
        if ("undefined" == typeof(extensionParams) || null == extensionParams["url"])
            return null;
        return extensionParams["url"] + "apps/";
    }

    function getAppPath(config) {
        var extensionPath = getExtensionPath(),
            path = extensionPath ? extensionPath : getBasePath(),
            appMap = {
                'text': 'documenteditor',
                'text-pdf': 'documenteditor',
                'spreadsheet': 'spreadsheeteditor',
                'presentation': 'presentationeditor'
            },
            app = appMap['text'];

        if (typeof config.documentType === 'string') {
            app = appMap[config.documentType.toLowerCase()];
        } else
        if (!!config.document && typeof config.document.fileType === 'string') {
            var type = /^(?:(xls|xlsx|ods|csv|xlst|xlsy|gsheet|xlsm|xlt|xltm|xltx|fods|ots)|(pps|ppsx|ppt|pptx|odp|pptt|ppty|gslides|pot|potm|potx|ppsm|pptm|fodp|otp))$/
                            .exec(config.document.fileType);
            if (type) {
                if (typeof type[1] === 'string') app = appMap['spreadsheet']; else
                if (typeof type[2] === 'string') app = appMap['presentation'];
            }
        }

        var userAgent = navigator.userAgent.toLowerCase(),
            check = function(regex){ return regex.test(userAgent); },
            isIE = !check(/opera/) && (check(/msie/) || check(/trident/) || check(/edge/)),
            isChrome = !isIE && check(/\bchrome\b/),
            isSafari_mobile = !isIE && !isChrome && check(/safari/) && (navigator.maxTouchPoints>0);

        path += app + "/";
        path += (config.type === "mobile" || isSafari_mobile)
            ? "mobile"
            : config.type === "embedded"
                ? "embed"
                : "main";

        var index = "/index.html";
        if (config.editorConfig) {
            var customization = config.editorConfig.customization;
            if ( typeof(customization) == 'object' && ( customization.toolbarNoTabs ||
                                                        (config.editorConfig.targetApp!=='desktop') && (customization.loaderName || customization.loaderLogo))) {
                index = "/index_loader.html";
            } else if (config.editorConfig.mode == 'editdiagram' || config.editorConfig.mode == 'editmerge')
                index = "/index_internal.html";

        }
        path += index;
        return path;
    }

    function getAppParameters(config) {
        var params = "?_dc=6.0.0-105&customer=XDATA%20DOC";

        if (config.editorConfig && config.editorConfig.lang)
            params += "&lang=" + config.editorConfig.lang;

        if (config.editorConfig && config.editorConfig.targetApp!=='desktop') {
           /* if ( (typeof(config.editorConfig.customization) == 'object') && config.editorConfig.customization.loaderName) {
                if (config.editorConfig.customization.loaderName !== 'none') params += "&customer=" + config.editorConfig.customization.loaderName;
            } else
                params += "&customer=XDATA%20DOC";*/
            if ( (typeof(config.editorConfig.customization) == 'object') && config.editorConfig.customization.loaderLogo) {
                if (config.editorConfig.customization.loaderLogo !== '') params += "&logo=" + config.editorConfig.customization.loaderLogo;
            } else if ( (typeof(config.editorConfig.customization) == 'object') && config.editorConfig.customization.logo) {
                if (config.type=='embedded' && config.editorConfig.customization.logo.imageEmbedded)
                    params += "&headerlogo=" + config.editorConfig.customization.logo.imageEmbedded;
                else if (config.type!='embedded' && config.editorConfig.customization.logo.image)
                    params += "&headerlogo=" + config.editorConfig.customization.logo.image;
            }
        }

        if (config.editorConfig && (config.editorConfig.mode == 'editdiagram' || config.editorConfig.mode == 'editmerge'))
            params += "&internal=true";

        if (config.frameEditorId)
            params += "&frameEditorId=" + config.frameEditorId;

        if (config.editorConfig && config.editorConfig.mode == 'view' ||
            config.document && config.document.permissions && (config.document.permissions.edit === false && !config.document.permissions.review ))
            params += "&mode=view";

        if (config.editorConfig && config.editorConfig.customization && !!config.editorConfig.customization.compactHeader)
            params += "&compact=true";

        if (config.editorConfig && config.editorConfig.customization && (config.editorConfig.customization.toolbar===false))
            params += "&toolbar=false";

        if (config.parentOrigin)
            params += "&parentOrigin=" + config.parentOrigin;

        return params;
    }

    function createIframe(config) {
        var iframe = document.createElement("iframe");

        iframe.src = getAppPath(config) + getAppParameters(config);
        iframe.width = config.width;
        iframe.height = config.height;
        iframe.align = "top";
        iframe.frameBorder = 0;
        iframe.name = "frameEditor";
        iframe.allowFullscreen = true;
        iframe.setAttribute("allowfullscreen",""); // for IE11
        iframe.setAttribute("onmousewheel",""); // for Safari on Mac
        iframe.setAttribute("allow", "autoplay");
        
		if (config.type == "mobile")
		{
			iframe.style.position = "fixed";
            iframe.style.overflow = "hidden";
            document.body.style.overscrollBehaviorY = "contain";
		}
        return iframe;
    }

    function postMessage(wnd, msg) {
        if (wnd && wnd.postMessage && window.JSON) {
            // TODO: specify explicit origin
            wnd.postMessage(window.JSON.stringify(msg), "*");
        }

    }

    function extend(dest, src) {
        for (var prop in src) {
            if (src.hasOwnProperty(prop)) {
                if (typeof dest[prop] === 'undefined') {
                    dest[prop] = src[prop];
                } else
                if (typeof dest[prop] === 'object' &&
                        typeof src[prop] === 'object') {
                    extend(dest[prop], src[prop])
                }
            }
        }
        return dest;
    }

})(window.DocsAPI = window.DocsAPI || {}, window, document);

