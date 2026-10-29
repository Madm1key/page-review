(function ($) {
    var REST = AJS.contextPath() + '/rest/page-review/1.0';

    function post(url) {
        return $.ajax({
            url: url,
            type: 'POST',
            dataType: 'json',
            headers: {'X-Atlassian-Token': 'no-check'}
        });
    }

    function errorText(xhr) {
        try {
            var body = JSON.parse(xhr.responseText);
            if (body && body.error) {
                return body.error;
            }
        } catch (e) { /* not json */ }
        return AJS.I18n.getText('page-review.error.generic');
    }

    function flag(type, title, body) {
        AJS.flag({type: type, title: title, body: body, close: 'auto'});
    }

    $(document).on('click', '#page-review-send', function (e) {
        e.preventDefault();
        var $btn = $(this);
        var pageId = AJS.params.pageId;
        if ($btn.attr('aria-disabled') === 'true' || !pageId) {
            return;
        }
        $btn.attr('aria-disabled', 'true').addClass('page-review-busy');
        post(REST + '/pages/' + encodeURIComponent(pageId) + '/send')
            .done(function (res) {
                if (res.unchanged) {
                    flag('info', AJS.I18n.getText('page-review.flag.unchanged.title'),
                        AJS.I18n.getText('page-review.flag.unchanged.body'));
                    return;
                }
                var $body = $('<p/>');
                $('<a target="_blank" rel="noopener noreferrer"/>')
                    .attr('href', res.mrUrl)
                    .text('!' + res.iid)
                    .appendTo($body);
                if (res.skipped) {
                    $body.append($('<br/>')).append(
                        document.createTextNode(AJS.I18n.getText('page-review.flag.skipped', res.skipped)));
                }
                flag('success', AJS.I18n.getText('page-review.flag.success.title'), $body.html());
            })
            .fail(function (xhr) {
                flag('error', AJS.I18n.getText('page-review.flag.error.title'),
                    $('<div/>').text(errorText(xhr)).html());
            })
            .always(function () {
                $btn.removeAttr('aria-disabled').removeClass('page-review-busy');
            });
    });

    $(document).on('click', '#page-review-test', function () {
        var $btn = $(this);
        var $result = $('#page-review-test-result').removeClass('page-review-ok page-review-fail').text('');
        $btn.prop('disabled', true);
        post(REST + '/spaces/' + encodeURIComponent($btn.data('space-key')) + '/test')
            .done(function () {
                $result.addClass('page-review-ok').text(AJS.I18n.getText('page-review.settings.test.ok'));
            })
            .fail(function (xhr) {
                $result.addClass('page-review-fail').text(errorText(xhr));
            })
            .always(function () {
                $btn.prop('disabled', false);
            });
    });
}(AJS.$));
