<!--
    Copyright (c) 2023 Contributors to the Eclipse Foundation

    See the NOTICE file(s) distributed with this work for additional
    information regarding copyright ownership.

    This program and the accompanying materials are made available under the
    terms of the Eclipse Public License 2.0 which is available at
    http://www.eclipse.org/legal/epl-2.0

    SPDX-License-Identifier: EPL-2.0
 -->
<template>
  <div>
    <el-dialog :visible.sync="threadTableVisible" width="60%" top="5vh">
      <thread :file="file" :id="selectedThreadId" :name="selectedThreadName" />
    </el-dialog>
    <h5 v-if="treeData.length==0">
      <i style="color: #67C23A" class="el-icon-success"></i> No Threads are blocked <el-icon><CircleCheckFilled /></el-icon>
    </h5>
    <span v-for="(item, index) in treeData" v-bind:key="index">
      <h5 style="margin-top: 15px; margin-bottom: 15px;"><i style="color: #F56C6C" class="el-icon-warning"></i> {{$tc('jifa.threadDump.blockedThreads.title', item.children.length, { count: item.children.length, blocker: item.name })}}</h5>
      <svg :cell="index" width="400" height="220">
        <g transform="translate(5, 5)">
          <g class="links"></g>
          <g class="nodes"></g>
        </g>
      </svg>
    </span>
  </div>
</template>

<script>
import axios from 'axios'
import { threadDumpService } from '@/util'
import Thread from "@/components/threaddump/Thread";
import * as d3 from "d3";

export default {
  props: ['file'],
  components: {
    Thread,
  },
  data() {
    return {
      treeData: [],
      threadTableVisible: false,
      selectedThreadId: null,
      selectedThreadName: null
    }
  },
  methods: {
    loadData() {
      axios.get(threadDumpService(this.file, "threadsBlocking"), {
        params: {
        }
      }).then(resp => {
        let data = resp.data
        let loaded = this.treeData
        if (loaded.length > 0) {
          // the last is summary row
          loaded.splice(loaded.length - 1, 1)
        }
        data.forEach(blockingThread => {
          loaded.push({
            name: blockingThread.blockingThread.name,
            value: 15,
            level: "#F56C6C",
            id: blockingThread.blockingThread.id,
            children: this.transformChildNodes(blockingThread)
          })
        })
        //init the trees in the next render loop after the v-for is expanded
        this.$nextTick(() => {
          this.initTree();
        })
      })
    },

    selectThreadId(id) {
      this.selectedThreadId = id
      this.selectedThreadName = null
      this.threadTableVisible = true
    },
    selectThreadName(name) {
      this.selectedThreadId = null
      this.selectedThreadName = name
      this.threadTableVisible = true
    },

    transformChildNodes(blockedThreads) {
      var children = [];
      for (let child of blockedThreads.blockedThreads) {

        children.push({
          name: child.name,
          level: "#409EFF",
          value: 10,
          id: child.id
        });
      }
      return children;
    },

    initTree() {
      let self = this

      this.treeData.forEach(function (value, i) {
        self.renderGraph(value, i)
      });

    },

    renderGraph(treeRoot, index) {
      let self = this
      // set the dimensions and margins of the diagram
      //const margin = { top: 20, right: 90, bottom: 30, left: 90 }
      const margin = { top: 0, right: 0, bottom: 0, left: 40 }

      const neededHeight = Math.max(60, (treeRoot.children.length * 25))
      const width = 660 - margin.left - margin.right
      const height = neededHeight - margin.top - margin.bottom;

      // declares a tree layout and assigns the size
      const treemap = d3.tree().size([height, 200]);

      //  assigns the data to a hierarchy using parent-child relationships
      let nodes = d3.hierarchy(treeRoot, d => d.children);

      // maps the node data to the tree layout
      nodes = treemap(nodes);

      // append the svg object to the body of the page
      // appends a 'group' element to 'svg'
      // moves the 'group' element to the top left margin
      const svg = d3.select("svg[cell='" + index + "']")
        .attr("width", width + margin.left + margin.right)
        .attr("height", height + margin.top + margin.bottom),
        g = svg.append("g")
          .attr("transform",
            "translate(" + margin.left + "," + margin.top + ")");

      // adds the links between the nodes
      g.selectAll(".link")
        .data(nodes.descendants().slice(1))
        .enter().append("path")
        .attr("class", "link")
        .attr("d", d => {
          return "M" + d.y + "," + d.x
            + "C" + (d.y + d.parent.y) / 2 + "," + d.x
            + " " + (d.y + d.parent.y) / 2 + "," + d.parent.x
            + " " + d.parent.y + "," + d.parent.x;
        });

      // adds each node as a group
      const node = g.selectAll(".node")
        .data(nodes.descendants())
        .enter().append("g")
        .attr("class", d => "node" + (d.children ? " node--internal" : " node--leaf"))
        .attr("transform", d => "translate(" + d.y + "," + d.x + ")");


      // adds the circle to the node
      node.append("circle")
        .attr("r", d => d.data.value)
        .style("fill", d => d.data.level)
        .on("click", function (e, d) {
          self.selectThreadId(d.data.id);
          //self.selectThreadName(d.data.name)
        });

      // adds the text to the node
      node.append("text")
        .attr("dy", ".35em")
        .attr("x", d => d.children ? -20 : 15)
        .attr("y", d => d.children ? -40 : 0)
        .style("text-anchor", d => d.children ? "start" : "start")
        .text(d => d.data.name)
        .on("click", function (e, d) {
          self.selectThreadId(d.data.id);
          //self.selectThreadName(d.data.name);
        });
    },
  },
  mounted() {
    this.loadData()
  },

};
</script>
<style>
.node circle {
  fill: #fff;
  stroke: steelblue;
  stroke-width: 1px;
}

.node text {
  font: 16px sans-serif;
}

.node:hover {
  stroke: #007bff;
  background-color: #007bff;
  stroke-width: 1px;
  cursor: pointer;
  font-weight: bold;
}

.link {
  fill: none;
  stroke: #ccc;
  stroke-width: 1px;
}
.link:hover {
  fill: none;
  stroke: black;
  stroke-width: 2px;
}
</style>
