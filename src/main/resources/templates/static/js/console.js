$(function () {
  /**dialog-->*/
  $('.dialog-close').click(function () {
    var dialog = $(this).closest('.dialog');
    dialog.css('display', 'none');
  });
  /**<--dialog*/

  /**search-->*/
  $('#search').click(function () {
    var inputKey = $('#input-key').val().trim();
    if (!checkString(inputKey)) {
      alert("key is empty");
      return;
    }

    var jsonRequest = {};
    jsonRequest['key'] = inputKey;

    request('/component/search', jsonRequest, function (data) {
      var tr = $('#component-table tr:eq(1)');
      var tdArray = tr.children();
      tdArray.each(function () {
        $(this).empty();
      });

      tdArray.eq(0).text(data.key);
      tdArray.eq(2).text(data.createTime);
      tdArray.eq(3).text(data.updateTime);

      if (data.structure == 1) {
        tdArray.eq(1).text(data.expression);
        tdArray.eq(4).text('-');
      } else if (data.structure == 2) {
        var jsonResponse = {};
        jsonResponse['initNumber'] = data.initNumber;
        jsonResponse['stepSize'] = data.stepSize;
        jsonResponse['resetType'] = data.resetType;

        tdArray.eq(1).text(JSON.stringify(jsonResponse));
        tdArray.eq(4).append(
            '<span class="dialog-component-update">' +
            ($('html').attr('lang') == 'zh' ? '更新' : 'Update')
            + '</span>');
      }
    });
  });
  /**<--search*/

  /**component create-->*/
  $('#dialog-create').click(function () {
    $('#create-component-cr-key').val('');
    $('#create-component-expression').val('');
    $('#create-component-ns-key').val('');
    $('#create-component-init-number').val('');
    $('#create-component-step-size').val('');
    $('#create-component-reset-type').val(0);

    $('#ns').prop('checked', false);
    $('#cr').prop('checked', true);
    $('#ns-component').css('display', 'none');
    $('#cr-component').css('display', '');

    $('#dialog-component-create').css('display', '');
  });

  $('#cr').click(function () {
    $('#ns').prop('checked', false);
    $('#cr').prop('checked', true);
    $('#ns-component').css('display', 'none');
    $('#cr-component').css('display', '');
  });

  $('#ns').click(function () {
    $('#cr').prop('checked', false);
    $('#ns').prop('checked', true);
    $('#cr-component').css('display', 'none');
    $('#ns-component').css('display', '');
  });

  $('#component-create').click(function () {
    var structure = $("input[name='component-type']:checked").val();

    var jsonRequest = {};
    jsonRequest['structure'] = structure;
    if (structure == 1) {
      var key = $('#create-component-cr-key').val().trim();
      var expression = $('#create-component-expression').val().trim();

      if (!checkString(key)) {
        alert("key is empty");
        return;
      }

      if (!checkString(expression)) {
        alert("expression is empty");
        return;
      }

      jsonRequest['key'] = key;
      jsonRequest['expression'] = expression;
    } else if (structure == 2) {
      var key = $('#create-component-ns-key').val().trim();
      var initNumber = $('#create-component-init-number').val().trim();
      var stepSize = $('#create-component-step-size').val().trim();
      var resetType = $('#create-component-reset-type option:selected').val();

      if (!checkString(key)) {
        alert("key is empty");
        return;
      }

      if (!checkNaturalInt(initNumber)) {
        alert("initNumber must be an integer greater than or equal to 0");
        return;
      }

      if (!checkPositiveInt(stepSize)) {
        alert("stepSize must be an integer greater than 0");
        return;
      }

      if (!checkFixedNumber(resetType)) {
        alert("resetType must be an integer between 0 and 3");
        return;
      }

      jsonRequest['key'] = key;
      jsonRequest['initNumber'] = initNumber;
      jsonRequest['stepSize'] = stepSize;
      jsonRequest['resetType'] = resetType;
    } else {
      return;
    }

    request('/component/create', jsonRequest, function (data) {
      $('#dialog-component-create').css('display', 'none');

      $('#component-total').text(data.total);
    });
  });
  /**component create-->*/

  /**component update-->*/
  $('#component-table').on('click', '.dialog-component-update', function () {
    var tdArray = $(this).closest('tr').children();
    $('#update-component-ns-key').val(tdArray.eq(0).text());
    var jsonObj = JSON.parse(tdArray.eq(1).text());
    $('#update-component-init-number').val(jsonObj.initNumber);
    $('#update-component-step-size').val(jsonObj.stepSize);
    $('#update-component-reset-type').val(jsonObj.resetType);

    $('#dialog-component-update').css('display', '');
  });

  $('#component-update').click(function () {
    var key = $('#update-component-ns-key').val().trim();
    var initNumber = $('#update-component-init-number').val().trim();
    var stepSize = $('#update-component-step-size').val().trim();
    var resetType = $('#update-component-reset-type').val().trim();

    if (!checkString(key)) {
      alert("key is empty");
      return;
    }

    if (!checkNaturalInt(initNumber)) {
      alert("initNumber must be an integer greater than or equal to 0");
      return;
    }

    if (!checkPositiveInt(stepSize)) {
      alert("stepSize must be an integer greater than 0");
      return;
    }

    if (!checkFixedNumber(resetType)) {
      alert("resetType must be an integer between 0 and 3");
      return;
    }

    var jsonRequest = {};
    jsonRequest['structure'] = 2;
    jsonRequest['key'] = key;
    jsonRequest['initNumber'] = initNumber;
    jsonRequest['stepSize'] = stepSize;
    jsonRequest['resetType'] = resetType;

    request('/component/update', jsonRequest, function (data) {
      $('#dialog-component-update').css('display', 'none');

      var tr = $('#component-table tr:eq(1)');
      var tdArray = tr.children();
      tdArray.each(function () {
        $(this).empty();
      });

      var jsonResponse = {};
      jsonResponse['initNumber'] = data.initNumber;
      jsonResponse['stepSize'] = data.stepSize;
      jsonResponse['resetType'] = data.resetType;

      tdArray.eq(0).text(data.key);
      tdArray.eq(1).text(JSON.stringify(jsonResponse));
      tdArray.eq(2).text(data.createTime);
      tdArray.eq(3).text(data.updateTime);
      tdArray.eq(4).append(
          '<span class="dialog-component-update">' +
          ($('html').attr('lang') == 'zh' ? '更新' : 'Update')
          + '</span>');
    });
  });
  /**component update-->*/

  /**weight update-->*/
  $('.dialog-weight').click(function () {
    var tr = $(this).closest('tr');
    var sid = tr.find('td:eq(0)').text();
    var weight = $(this).text();

    $('#update-weight-sid').val(sid);
    $('#update-weight-value').val(weight);
    $('#update-weight-row').val(tr.index());

    $('#dialog-weight-update').css('display', '');
  });

  $('#weight-update').click(function () {
    var jsonRequest = {};
    jsonRequest['sid'] = $('#update-weight-sid').val();
    jsonRequest['weight'] = $('#update-weight-value').val();

    request('/weight/update', jsonRequest, function (data) {
      var tr = $('#route-table tr').eq($('#update-weight-row').val());
      var td = tr.find('td:eq(5)');
      td.children('.dialog-weight').text(data.weight);

      $('#dialog-weight-update').css('display', 'none');
    });
  });
  /**<--weight update*/

  /**block detail-->*/
  $('.block-detail').click(function () {
    var tr = $(this).closest('tr');
    var sid = tr.find('td:eq(0)').text();

    var jsonRequest = {};
    jsonRequest['sid'] = sid;

    request('/block/query', jsonRequest, function (data) {
      $('#dialog-block-detail table tr:gt(0)').remove();

      var trRow = '';
      data.forEach(function (item) {
        trRow += '<tr>';
        trRow += '<td>' + item.blockIndex + '</td>';
        trRow += '<td>' + item.componentCount + '</td>';
        trRow += '<td>' + item.beginPhyOffset + '</td>';
        trRow += '<td>' + item.endPhyOffset + '</td>';
        trRow += '<td>' + item.beginTimestamp + '</td>';
        trRow += '<td>' + item.endTimestamp + '</td>';
        trRow += '</tr>';
      });
      $('#dialog-block-detail table tbody').append(trRow);

      $('#dialog-block-detail').css('display', '');
    });
  });
  /**<--block detail*/

  /**kick out-->*/
  $('.dialog-kick-out').click(function () {
    var tr = $(this).closest('tr');
    var sid = tr.find('td:eq(0)').text();

    $('#kick-out-sid').text(sid);
    $('#kick-out-row').val(sid);

    $('#dialog-node-kick-out').css('display', '');
  });

  $('#kick-out').click(function () {
    var jsonRequest = {};
    jsonRequest['sid'] = $('#kick-out-row').val();

    request('/node/kickOut', jsonRequest);
  });
  /**<--kick out*/

  /**remove-->*/
  $('#route-table').on('click', '.dialog-remove', function () {
    var tr = $(this).closest('tr');
    var sid = tr.find('td:eq(0)').text();

    $('#remove-sid').text(sid);
    $('#remove-row').val(sid);

    $('#dialog-node-remove').css('display', '');
  });

  $('#remove').click(function () {
    var jsonRequest = {};
    jsonRequest['sid'] = $('#remove-row').val();

    request('/node/remove', jsonRequest);
  });
  /**<--remove*/
});

var jqxhr;
$.ajaxSetup({
  complete: function () {
    if ('redirect' == jqxhr.getResponseHeader('redirect')) {
      var win = window;
      while (win != win.top) {
        win = win.top;
      }
      win.location.href = jqxhr.getResponseHeader("redirectUrl");
    }
  }
});

function request(url, d, callback) {
  jqxhr = $.ajax({
    url: url,
    type: 'post',
    data: JSON.stringify(d),
    contentType: 'application/json',
    async: true,
    success: function (data) {
      var body = processResp(data);
      if (typeof body != "undefined" && body != null && body != "") {
        if (typeof callback != "undefined") {
          callback(body);
        }
      }
    }
  });
}

function processResp(data) {
  if (data.code == 1) {
    return data.body;
  } else {
    if (typeof data != "undefined" && data != null && data != "") {
      alert(data.callOut);
    }
  }
}

function checkString(v) {
  if (typeof v != "undefined" && v != null && v != "") {
    return true;
  }

  return false;
}

function checkPositiveInt(v) {
  var reg = new RegExp('^\\+?[1-9][0-9]*$');
  return reg.test(v);
}

function checkNaturalInt(v) {
  var reg = new RegExp('^\\d+$');
  return reg.test(v);
}

function checkFixedNumber(v) {
  if (v == 0 || v == 1 || v == 2 || v == 3) {
    return true;
  }

  return false;
}