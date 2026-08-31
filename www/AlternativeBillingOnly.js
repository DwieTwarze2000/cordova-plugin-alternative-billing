var exec = require('cordova/exec')

function execPromise(action) {
  return new Promise(function (resolve, reject) {
    exec(resolve, reject, 'AlternativeBillingOnly', action, [])
  })
}

module.exports = {
  connect: function () {
    return execPromise('connect')
  },

  isAvailable: function () {
    return execPromise('isAvailable')
  },

  showInfoDialog: function () {
    return execPromise('showInfoDialog')
  },

  getReportingToken: function () {
    return execPromise('getReportingToken')
  },
}
