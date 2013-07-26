/*global $, jQuery*/

ConfigAdmin.controller(
    "ConfigAdminController", function( $scope, $location, $routeParams, $rootScope, $route, Config){
        render = function(){
            if($rootScope.schemas == undefined){
                Config.getSchemas().success(function(schemas){
                    $rootScope.schemas = schemas;
                });
            }

            var renderAction = $route.current.action;
            if(renderAction == undefined) {
                return;
            }

            var renderPath = renderAction.split( "." );
            $scope.renderAction = renderAction;
            $scope.renderPath = renderPath;
            $scope.topNavActive = renderPath[ 0 ];
            $scope.leftNavActive = renderPath[ 1 ];
            $scope.pathParams = $route.current.pathParams;
        };

        $scope.create = function(schema) {
            $location.path('/bean-create/' + schema.schemaName).replace();
        };

        $scope.$on(
            "$routeChangeSuccess",
            function( $currentRoute, $previousRoute ){
                render();
                $scope.$broadcast( "requestContextChanged" );
            }
        );
    }
);