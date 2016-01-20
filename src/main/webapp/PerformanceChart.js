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

    var stacked = this.stackCash(this.props.cash, this.props.values)
    var data = [
      {x: this.props.dateRange, y: this.props.cash, type: 'scatter', fill: 'tozeroy', fillcolor: 'blue' , line : { color: 'blue'}, name : 'Cash' },
      {x: this.props.dateRange, y: stacked, type: 'scatter', fill: 'tonexty', fillcolor: 'lightblue'  , line : { color: 'lightblue'} , name : 'Total Value' },
      {x: this.props.dateRange, y: this.props.investment, type: 'scatter', line : { color: 'grey'}, name : 'Investment' }
    ]
    var layout = {
        title: 'Portfolio',
        showlegend: true
    }
    Plotly.newPlot('portfolioChart', data, layout, {displayModeBar: false})
  }
})