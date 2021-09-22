window.Doc = {
    load : function(success, fail){
        let id = this.getId();
        if (id){
            this.getModel(id, success, fail);
        }else{
            let msg = "请指定文档id！";
            if (fail) {
                fail(msg);
            }else{
                console.error(msg);
            }
        }
    },
    getStreamUrl:function(doc){
        return "./download/" + doc.id;
    },
    getId : function(){
        return this.getParam("id");
    },
    getParam : function(key){
        let value = null, searchStr = location.search;
        if (searchStr.length > 1){
            let params = searchStr.substring(1).split("&");
            if (params.length > 0){
                params.forEach(function(item){
                    let kv = item.split("=");
                    if (kv[0] === key){
                        value = kv[1];
                    }
                });
            }
        }
        return value;
    },
    getModel : function(id, success, fail){
        let params = [];
        let platform = this.getParam("platform");
        if (platform && platform.length > 0){
            params.push("platform=" + platform);
        }
        let mode = this.getParam("mode");
        if (mode && mode.length > 0){
            params.push("mode=" + mode);
        }
        fetch(`./rest/rs/v1/model/${id}?${params.join("&")}`, {
            credentials: 'include'
        }).then(function(resp){
            return resp.json();
        }).then(function(result) {
            let data = result.data;
            if (data.name){
                document.title = data.name;
            }
            success(data);
        }).catch(function(ex){
            if (fail){
                fail(ex);
            }else{
                console.error(ex);
            }
        });
    },
    excludeUrls:[ "http://127.0.0.1", "http://127.0.0.1/", "http://localhost", "http://localhost/" ],
    getExternal: function(success, fail){
        let type = this.getParam("type");
        if (!type){
            console.error("请指定文档类型!");
            return;
        }
        let url = this.getParam("url");
        if (!url || this.excludeUrls.indexOf(url) > -1){
            console.error("请指定文档地址!");
            return;
        }
        let platform = this.getParam("platform");
        if (platform && platform.length > 0){
            platform = "platform=" + platform + "&";
        }else{
            platform = "";
        }
        fetch(`./external?${platform}type=${type}&url=${url}`, {
            credentials: 'include'
        }).then(function(resp){
            return resp.json();
        }).then(function(result) {
            success(result.data);
        }).catch(function(ex){
            if (fail){
                fail(ex);
            }else{
                console.error(ex);
            }
        });
    }
};