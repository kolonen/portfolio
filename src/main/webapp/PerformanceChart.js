var React = require('react')
var Plotly = require('plotly.js')
var _ = require('lodash')

module.exports = React.createClass({
  render: function() {
    return React.DOM.div({id: 'portfolioChart', style: {height: "700px"}});
  },
  componentDidMount: function() {
    this.drawChart();
  },
  componentDidUpdate: function() {
    this.drawChart();
  },
  stackCash: function(cash, values) {
    return _.zipWith(cash, values, function(c, v) { return c + v })
  },
  drawChart: function() {

    var dates = _.map(this.props.balances, function(d) { return d.date })
    var cashSeries = _.map(this.props.balances, function(d) { return parseFloat(d.cash) })
    var valueSeries = _.map(this.props.values, function(d) { return parseFloat(d.value) })
    var investmentSeries = _.map(this.props.balances, function(d) { return parseFloat(d.investment) })
    var stacked = this.stackCash(valueSeries, cashSeries)
    var data = [
      {x: dates, y: cashSeries, type: 'scatter', fill: 'tozeroy', fillcolor: 'lightgreen' , line : { color: 'green'}},
      {x: dates, y: stacked, type: 'scatter', fill: 'tonexty', fillcolor: 'lightorange' , line : { color: 'lightorange'}},
      {x: dates, y: investmentSeries, type: 'scatter', line : { color: 'red'}}
    ]
    var layout = {
        title: 'Portfolio',
        showlegend: true
    }
    Plotly.newPlot('portfolioChart', data, layout, {displayModeBar: false})
  }
})