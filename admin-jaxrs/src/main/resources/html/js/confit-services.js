/*jslint browser: true*/
/*global $, jQuery*/

angular.module('confit-module', ['ngResource']).
    factory('Config', function($resource, $http, $q){
        var baseUrl = '/confit-admin';
        return {
            createConfig: function(className, bean) {
                var url = baseUrl + '/create';
                return $http.post(url, { className: className, bean: bean });
            },
            setConfig: function(className, bean) {
                var url = baseUrl + '/set';
                return $http.post(url, { className: className, bean: bean });
            },
            listConfig: function(className, callback){
                var url = baseUrl + '/list/' + className;
                return $http.get(url);
            },
            getConfig: function(className, id) {
                var url = baseUrl + '/get/' + className + "/" + id;
                return $http.get(url);
            },
            getConfigs: function(className, ids) {
                var url = baseUrl + '/get/' + className;
                return $http({
                    method: 'GET',
                    url: url,
                    params: {
                        id: ids
                    }
                });
            },
            paginateConfig: function(className, start, max, prop, schema, id){
                var url = baseUrl + '/paginate/' + className;
                var deferred = $q.defer();
                $http({
                    url: url,
                    method: 'GET',
                    params: {
                        first: start,
                        max: max,
                        prop: prop,
                        schema: schema,
                        id: id
                    }
                }).success(function(result){
                        deferred.resolve(result);
                    }).error(function(result, status){
                        deferred.reject(result, status);
                    });
                return deferred.promise;
            },
            deleteConfig: function(className, id){
                var url = baseUrl + '/delete/' + className + "/" + id;
                return $http({
                    url: url,
                    method: 'DELETE'
                });
            },
            getSchemas: function() {
                var url = baseUrl + '/getSchemas';
                return $http({
                    method: 'GET',
                    url: url
                });
            },
            createBean: function(bean) {
                var url = baseUrl + '/createBean';
                return $http.post(url, bean);
            },
            deleteBean: function(schemaName, id) {
                var url = baseUrl + '/deleteBean/'+schemaName +"/"+id;
                return $http({
                    url: url,
                    method: 'DELETE',
                    headers: {
                        // DELETE set content-type to xml and
                        // this override does not work for some reason?
                        'Content-Type': 'application/json'
                    }
                });
            },
            getBean: function(schemaName, id) {
                var url = baseUrl + '/getBean/' + schemaName + "/" + id;
                return $http.get(url);
            },
            listBeans: function(schemaName){
                var url = baseUrl + '/listBeans/' + schemaName;
                return $http.get(url);
            },
            paginateBeans: function(schemaName, start, max){
                var url = baseUrl + '/query/' + schemaName;
                var deferred = $q.defer();
                $http({
                    url: url,
                    method: 'GET',
                    params: {
                        first: start,
                        max: max
                    }
                }).success(function(result){
                        deferred.resolve(result);
                    }).error(function(result, status){
                        deferred.reject(result, status);
                    });
                return deferred.promise;
            },
            setBean: function(bean) {
                var url = baseUrl + '/setBean';
                return $http.post(url, bean);
            }
        }
    }
);