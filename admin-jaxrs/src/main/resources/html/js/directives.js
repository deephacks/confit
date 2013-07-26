ConfigAdmin.directive('qnValidate', [
    function() {
        return {
            link: function(scope, element, attr) {
                var form = element.inheritedData('$formController');
                // no need to validate if form doesn't exists
                if (!form) return;
                // validation model
                var validate = attr.qnValidate;
                // watch validate changes to display validation
                scope.$watch(validate, function(errors) {
                    // every server validation should reset others
                    // note that this is form level and NOT field level validation
                    form.$myServerErrors = { };

                    // loop through errors
                    angular.forEach(errors, function(error, i) {
                        form.$myServerErrors[error.key] = {
                            $invalid: true,
                            httpStatus: error.httpStatus,
                            msg: error.msg
                        };
                    });
                });
            }
        };
    }
]);

ConfigAdmin.directive('rightClick', function($parse) {
    return function(scope, element, attr) {
        element.bind('contextmenu', function(event) {
            event.preventDefault();
            var fn = $parse(attr['rightClick']);
            scope.$apply(function() {
                fn(scope, {
                    $event: event
                });
            });
            return false;
        });
    }
});