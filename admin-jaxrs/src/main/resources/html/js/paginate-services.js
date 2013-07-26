/*jslint browser: true*/
/*global $, jQuery*/

angular.module('paginate-module', []).
    factory('Paginate', function() {
        return {
            range: function(start, end){
                var ret = [];
                if (!end) {
                    end = start;
                    start = 0;
                }
                for (var i = start; i < end; i++) {
                    ret.push(i);
                }
                return ret;
            },
            prevPage: function (query, paginate) {
                if (query.currentPage > 0) {
                    query.currentPage--;
                    paginate(query);
                }

            },
            nextPage: function (query, paginate) {
                if (query.currentPage < query.numPages - 1) {
                    query.currentPage++;
                    paginate(query);
                }
            }
        };
    }
);
