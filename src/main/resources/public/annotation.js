/**
 * Created by Fran Lozano
 */

//Hide second and third block
$("input[name^='rumor']").closest('.switch-block').hide();
$("input[name^='topic']").closest('.switch-block').hide();


$(document).ready(function() {
    $(':checkbox').change(function() {
        var switchChild;
        var name = $(this).attr("name");
            if (name.startsWith("assertion")) {
                switchChild =  $(this).closest('.switches').find("input[name^='topic']").prop('checked', false);
                switchChild.closest('.switch-block').toggle();
                switchChild =  $(this).closest('.switches').find("input[name^='rumor']").prop('checked', false);
                switchChild.closest('.switch-block').hide();
            } else if (name.startsWith("topic")){
                switchChild =  $(this).closest('.switches').find("input[name^='rumor']").prop('checked', false);
                switchChild.closest('.switch-block').toggle();
            }
    });
});