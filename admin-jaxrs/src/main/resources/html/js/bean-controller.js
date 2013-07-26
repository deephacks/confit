/*jslint browser: true*/
/*global $, jQuery*/

ConfigAdmin.controller(
    "bean-list-controller", function ($scope, $location, $routeParams, Config, Paginate) {
        $scope.schemaName = $routeParams.schemaName;

        $scope.delete = function(schemaName, id){
            Config.deleteBean(schemaName, id).
                success(function(){
                    $scope.$broadcast( "requestContextChanged" );
                }).error(function(data, status){
                    $scope.errors = [
                        { key: 'bean', httpStatus: status, msg: data }
                    ];
                });
        }

        $scope.paginate = function ( query ) {
            var max = 20;
            var start = query.currentPage * max;

            Config.paginateBeans($routeParams.schemaName, start, max).then(
                function(beans){
                    query.numPages = Math.ceil(beans.totalCount / max);
                    $scope.beans = beans.beans;
                });
        }
        $scope.setPage = function (query) {
            query.currentPage = this.n;
            $scope.paginate(query);
        }
        $scope.range = function (start, end){
            return Paginate.range(start, end);
        }
        $scope.prevPage = function (query) {
            Paginate.prevPage(query, $scope.paginate);
        }
        $scope.nextPage = function (query) {
            Paginate.nextPage(query, $scope.paginate);
        }
        if($scope.query == undefined){
            query = {
                currentPage: 0,
                numPages: 0
            };
            $scope.query = query;
            $scope.paginate(query);
        }


        $scope.$on(
            "requestContextChanged",
            function() {
                $scope.schemaName = $scope.pathParams.schemaName;
                $scope.errors = [];
                Config.paginateBeans($scope.pathParams.schemaName, 0, 20).then(
                    function(beans){
                        $scope.query.numPages = Math.ceil(beans.totalCount / 20);
                        $scope.beans = beans.beans;
                    });
            }
        );
    }
);

ConfigAdmin.controller(
    "bean-detail-controller", function ($scope, $location, Property, $routeParams, Config) {
        $scope.id = $routeParams.id;
        $scope.schemaName = $routeParams.schemaName;
        var schema = $scope.schemas[$scope.schemaName];
        Config.getBean($scope.schemaName, $scope.id ).success(function(bean){
            $scope.bean = bean;
            $scope.dirty = {};
            for(var i = 0; i < schema.propertyNames.length; i++){
                var propertyName = schema.propertyNames[i];
                $scope.dirty[propertyName] = false;
            }
        });
        $scope.editList = function(name) {
            Property.editList(name, $scope.bean, $scope.dirty);

        }

        $scope.edit = function(name) {
            Property.edit(name, $scope.bean, $scope.dirty);
        }

        $scope.clear = function(name) {
            Property.clear(name, $scope.bean, $scope.dirty);
        }

        $scope.add = function(name, value) {
            Property.add(name, value, $scope.bean, $scope.dirty);
        }

        $scope.remove = function(name, pos) {
            Property.remove(name, pos, $scope.bean, $scope.dirty);
        }

        $scope.isDirty = function(){
            return Property.isDirty($scope.dirty);
        }

        $scope.commit = function() {
            if(!$scope.isDirty()){
                return;
            }
            // all properties are arrays - need to convert
            // single property fields to single element array
            for(var key in $scope.bean.properties){
                if( typeof $scope.bean.properties[key] === 'string' ) {
                    $scope.bean.properties[key] = [$scope.bean.properties[key]];
                }
            }
            Config.setBean($scope.bean).success(function(){
                var redirect = '/bean-list/'+$scope.schemaName;
                $location.path(redirect);
            }).error(function(data, status){
                    $scope.errors = [
                        { key: 'bean', httpStatus: status, msg: data }
                    ];
                });
        }

        $scope.rollback = function(){
            var redirect = '/bean-list/'+$scope.schemaName;
            $location.path(redirect);
        }
    }
);

ConfigAdmin.controller(
    "bean-create-controller", function ($scope, $location, $routeParams, Config, Property) {
        $scope.schemaName = $routeParams.schemaName;
        var schema = $scope.schemas[$scope.schemaName];
        $scope.bean = {
            properties: {},
            schemaName: $scope.schemaName
        };
        $scope.dirty = {};
        for(var i = 0; i < schema.propertyNames.length; i++){
            var propertyName = schema.propertyNames[i];
            $scope.dirty[propertyName] = false;
            $scope.bean.properties[propertyName] = [];
        }

        $scope.editList = function(name) {
            Property.editList(name, $scope.bean, $scope.dirty);
        }

        $scope.edit = function(name) {
            Property.edit(name, $scope.bean, $scope.dirty);
        }

        $scope.clear = function(name) {
            Property.clear(name, $scope.bean, $scope.dirty);
        }

        $scope.add = function(name, value) {
            Property.add(name, value, $scope.bean, $scope.dirty);
        }

        $scope.remove = function(name, pos) {
            Property.remove(name, pos, $scope.bean, $scope.dirty);
        }

        $scope.isDirty = function(){
            Property.isDirty($scope.dirty);
        }

        $scope.create = function(bean) {
            for(var key in $scope.bean.properties){
                if( typeof $scope.bean.properties[key] === 'string' ) {
                    $scope.bean.properties[key] = [$scope.bean.properties[key]];
                }
            }
            Config.createBean($scope.bean).
                success(function(){
                    $location.path('/bean-list/' + $scope.schemaName);
                    $scope.$broadcast( "requestContextChanged" );
                }).error(function(data, status){
                    $scope.errors = [
                        { key: 'bean', httpStatus: status, msg: data }
                    ];
                });
        }

    }
);



