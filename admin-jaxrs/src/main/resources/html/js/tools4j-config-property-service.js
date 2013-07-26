/**
 *
 */

/*jslint browser: true*/
/*global $, jQuery*/

angular.module('tools4j-config-property-module', ['ngResource']).
    factory('Property', function($resource, $http, $q){
        return {
            editList: function(name, bean, dirty) {
                dirty[name] = true;

                if(bean.properties[name] == undefined){
                    bean.properties[name] = [];
                }
            },
            edit: function(name, bean, dirty) {
                dirty[name] = true;

                if(bean.properties[name] == undefined){
                    bean.properties[name] = [''];
                }
            },

            clear: function(name, bean, dirty) {
                dirty[name] = true;
                bean.properties[name] = undefined;
            },

            add: function(name, value, bean, dirty) {
                dirty[name] = true;
                if(bean.properties[name] == undefined){
                    bean.properties[name] = [];
                }
                bean.properties[name].push(value);
            },

            remove: function(name, pos, bean, dirty) {
                dirty[name] = true;
                if(bean.properties[name] == undefined){
                    return;
                }
                bean.properties[name].splice(pos, 1);
            },

            isDirty: function(dirty){
                if(dirty == undefined){
                    return false;
                }
                for(var key in dirty){
                    if(dirty[key]){
                        return true;
                    }
                }
            }

        }
    }
);