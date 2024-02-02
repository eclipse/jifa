/********************************************************************************
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License 2.0 which is available at
 * http://www.eclipse.org/legal/epl-2.0
 *
 * SPDX-License-Identifier: EPL-2.0
 ********************************************************************************/
{
  const template = document.createElement('template');

  template.innerHTML = `
        <style>
            .none-pinter-events {
                pointer-events: none;
            }
            
            #flame-graph {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
                font-family: Menlo, NotoSans, 'Lucida Grande', 'Lucida Sans Unicode', sans-serif;
                line-height: normal;
                
                display: flex;
                flex-direction: row-reverse;
                overflow-y: visible;
            }
            
            #flame-graph-inner {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                
                flex-grow: 1;
            }
            
            #flame-graph-inner-wrapper {
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
            }

            #flame-graph-canvas {
                display: block;
                position: relative;
                top: 0;
                left: 0;
                margin: 0;
                padding: 0;
                width: 100%;
                height: 100%;
            }

            #pinned-frame-mask, #frame-mask {
                position: absolute;
                top: 0;
                left: 0;
                width: 0;
                height: 0;
                outline: 2px black solid ;
                outline-offset: -2px;
                visibility: hidden;
            }

            #frame-mask-text {
                position: absolute;
                left: 0;
                top: 0;
            }

            #frame-postcard-wrapper{
                position: fixed;
                top: 0;
                left: 0;
                z-index: 1
            }

            #frame-postcard {
                position: absolute;
                top: 0;
                left: 0;
                visibility: hidden;
            }

            #frame-postcard-starting-pointer {
                position: absolute;
                width: 6px;
                height: 6px;
                background-color: rgba(55, 59, 70, .8);
                border-radius: 100%;
                display: block;
                transform: translate3d(-3px, -3px, 0);
            }

            #frame-postcard-connecting-line {
                position: absolute;
                width: 40px;
                height: 1px;
                background-color: rgba(55, 59, 70, .6);
                background-size: 100% 100%;
                display: block;
                transform-origin: 0 0;
            }

            #frame-postcard-content {
                position: absolute;
            }

            #frame-postcard-content-main {
                position: relative;
                width: fit-content;
                max-width: 338px;
                padding: 8px 8px 8px 20px;
                box-shadow: 0 0 1px rgba(0, 0, 0, .1), 0 2px 5px;
                border-radius: 6px;
            }

            #frame-postcard-content-main-title {
                font-weight: 700;
                font-size: 14px;
                word-wrap: break-word;
            }

            #frame-postcard-content-foot {
                width: 350px;
                line-height: 23px;
                padding: 8px;
                font-size: 14px;
                margin-top: 2px;
                border-radius: 6px;
            }
            
            #frame-postcard-shadow {
                position: absolute;
                top: 0;
                left: 0;
            }
            
            .keyboard {
                background-color: rgb(243, 243, 243);
                color: rgb(33, 33, 33);
                padding: 1px 4px 1px 4px;
                border-radius: 3px;
                border: solid 1px #ccc;
                border-bottom-color: #bbb;
                box-shadow: inset 0 -1px 0 #bbb;
            }
            
            #help-button {
                position: absolute;
                top: 0;
                left: 0;
                color: rgba(0, 0, 0, .6);
                font-size: 24px;
                cursor: pointer;
                visibility: hidden;
                width: 15px;
                height:24px;
            }
            
            #help-button:hover {
                background: rgba(0, 0, 0, .1);
            }
            
            #color-bar-wrapper {
                background: linear-gradient(to top, rgba(255, 0, 0, .75), rgba(0, 0, 0, .75) 50%, rgba(0, 128, 0, .75));
                width: 40px;
                height: 95%;
                display: flex;
                justify-content: space-around;
                padding: 3px;
                
                position: relative;
            }
            
            #color-bar {
                background: linear-gradient(to top, green, grey 50%, red);
                opacity: 1;
                width: 100%;
                height: 100%;
                display: flex;
                flex-direction: column;
                align-items: center;
                
                position: relative;
            }
            
            .color-bar-percent {
                display: flex;
                flex-direction: row;
                font-size: 12px;
                color: rgba(255, 255, 255, 1);
                font-weight: bold;
            }
            
            .color-bar-percent-positive {
                height: 12%;
                align-items: start;
                padding-top: 8px;
            }
            
            .color-bar-percent-negative {
                height: 12%;
                align-items: end;
                padding-bottom: 8px;
            }
            
            .color-bar-percent-0 {
                height: 4%;
                align-items: center;
            }
            
            #color-arrow {
                position: absolute;
                top: 50%;
                left: 44px;
                height: 8px;
                width: 8px;
                display: inline-block;
                background: linear-gradient(to top left, rgba(255, 255, 255, 0) 50%, rgba(55, 59, 70, .9) 50%, rgba(55, 59, 70, .9));
                transform-origin: top left;
                transform: rotate(-45deg);
                visibility: hidden;
            }
        </style>
            
        <div id="flame-graph">
            <div id='color-bar-div' style="width: 58px; flex-shrink: 0; display: none; align-items: center;">
                <div id='color-bar-wrapper'> 
                    <div id='color-bar'> 
                        <div class="color-bar-percent color-bar-percent-positive">+100%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+75%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+50%</div>
                        <div class="color-bar-percent color-bar-percent-positive">+25%</div>
                        <div class="color-bar-percent color-bar-percent-0">±0%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-25%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-50%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-75%</div>
                        <div class="color-bar-percent color-bar-percent-negative">-100%</div>
                        <div id="color-arrow"></div>
                    </div>
                </div>
            </div>
            
            <div id="flame-graph-inner">
                <div id="flame-graph-inner-wrapper">
                    <canvas id="flame-graph-canvas"/>
                </div>

                <div id="pinned-frame-mask" class="none-pinter-events"></div>

                <div id="frame-mask" tabindex="-1">
                    <div id="frame-mask-text"></div>
                </div>
            </div>
            
            <div id="frame-postcard-wrapper">
                <div id="frame-postcard" class="none-pinter-events">
                    <div id="frame-postcard-starting-pointer" class="none-pinter-events"></div>

                    <div id="frame-postcard-connecting-line" class="none-pinter-events"></div>

                    <div id="frame-postcard-content" class="none-pinter-events">
                        <div id="frame-postcard-content-main" class="none-pinter-events">
                            <div id="frame-postcard-content-main-line"
                                 style="position: absolute; left: 9px; top: 10px; bottom: 10px; width: 3px;
                                        border-radius: 6px;"
                                 class="none-pinter-events">
                            </div>
                            <span id="frame-postcard-content-main-title"></span>
                        </div>
                        <div id="frame-postcard-content-foot" class="none-pinter-events"></div>
                    </div>
                </div>
            </div>
            
            <div id="frame-postcard-shadow"></div>
            
            <div id="help-button">
                <svg style="margin-left: -4.5px" focusable="false" width="1em" height="1em" fill="currentColor" aria-hidden="true" viewBox="64 64 896 896">
                    <path d="M456 231a56 56 0 10112 0 56 56 0 10-112 0zm0 280a56 56 0 10112 0 56 56 0 10-112 0zm0 280a56 56 0 10112 0 56 56 0 10-112 0z"></path>
                </svg>
            </div>
            
            <div id="flame-graph-help"
              style="position: absolute;
                     width: 450px;
                     top: 36px; left: 50%;
                     margin-left: -240px;
                     background-color: rgba(0,0,0,0.8);color: white;
                     border-radius: 6px;
                     padding: 15px;
                     z-index: 9999;
                     visibility: hidden;">
              <div style="display: flex; justify-content: space-between; padding: 0 2px; font-size: 14px">
                <span>Flame Graph Help</span>
                <span id="close-flame-graph-help" style="cursor: pointer">x</span>
              </div>
              
              <div style="display: block; height: 1px; width: 100%; background-color: #9a9a9a; margin: 15px 0"></div>
              
              <div style="font-size: 12px">
                <div style="display: flex;">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                    <span class="keyboard">^c</span>, <span class="keyboard">⌘c</span>, <span class="keyboard">ff</span>
                  </div>
                  <div style="width: 70%;">Copy the content of the touched frame</div>
                </div>
                
                <div style="display: flex; padding-top: 15px">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                    <span class="keyboard">fs</span>
                  </div>
                  <div style="width: 70%;">Copy the stack trace from the touched frame</div>
                </div>
              
                <div style="display: flex; padding-top: 15px">
                  <div style="width: 30%; text-align: right; padding-right: 10px;">
                  Downward
                  </div>
                  <div style="width: 70%;">
                      <div id="downward-button" style="width: 28px; height: 16px; border-radius: 100px; cursor: pointer; display: flex; align-items: center">
                          <div style="width: 12px; height: 12px; border-radius: 100%; background: white; margin: 0 2px"></div>
                      </div>
                  </div>
                </div>
              <div/>
            </div>
        </div>
    `;

  function Frame(flameGraph, raw, depth, isRoot = false) {
    this.fg = flameGraph;
    this.isRoot = isRoot;

    this.raw = raw;

    this.weight = 0;
    this.selfWeight = 0;
    this.parent = null;
    this.index = -1;
    this.hasLeftSide = false;
    this.hasRightSide = false;
    this.depth = depth;

    this.weightOfBaseline1 = 0;
    this.selfWeightOfBaseline1 = 0;
    this.weightOfBaseline2 = 0;
    this.selfWeightOfBaseline2 = 0;

    this.text = '';

    this.addWeight = function (weight) {
      this.weight += weight;
    };

    this.addWeightOfBaselines = function (weightOfBaseline1, weightOfBaseline2) {
      this.weightOfBaseline1 += weightOfBaseline1;
      this.weightOfBaseline2 += weightOfBaseline2;
    };

    this.addSelfWeightOfBaselines = function (selfWeightOfBaseline1, selfWeightOfBaseline2) {
      this.selfWeightOfBaseline1 += selfWeightOfBaseline1;
      this.selfWeightOfBaseline2 += selfWeightOfBaseline2;
    };

    this.addSelfWeight = function (weight) {
      this.selfWeight += weight;
    };

    this.setPinned = function () {
      this.pinned = true;
      if (this.parent !== this.fg.$root) {
        this.parent.setPinned();
      }
    };

    this.setSide = function (left) {
      if (left) {
        this.parent.hasLeftSide = true;
      } else {
        this.parent.hasRightSide = true;
      }
    };

    this.clearSide = function (left) {
      if (left) {
        this.parent.hasLeftSide = false;
      } else {
        this.parent.hasRightSide = false;
      }
    };

    this.clearFindSide = function () {
      if (this.fg.$pinnedFrameLeft) {
        this.fg.$pinnedFrameLeft.clearSide(true);
        this.fg.$pinnedFrameLeft = null;
      }

      if (this.fg.$pinnedFrameRight) {
        this.fg.$pinnedFrameRight.clearSide(false);
        this.fg.$pinnedFrameRight = null;
      }
    };

    this.findSide = function () {
      let n = this;
      let p = this.parent;
      while (n.index === 0) {
        if (p === this.fg.$root) {
          break;
        }
        n = p;
        p = p.parent;
      }

      if (n.index > 0) {
        let t = p.children[n.index - 1];
        this.fg.$pinnedFrameLeft = t;
        t.setSide(true);
      }

      n = this;
      p = this.parent;
      while (n.index === p.children.length - 1) {
        if (p === this.fg.$root) {
          break;
        }
        if (p.selfWeight > 0) {
          return;
        }
        n = p;
        p = p.parent;
      }

      if (n.index < p.children.length - 1) {
        let t = p.children[n.index + 1];
        this.fg.$pinnedFrameRight = t;
        t.setSide(false);
      }
    };

    this.setUnpinned = function () {
      this.pinned = false;
      if (this.parent !== this.fg.$root) {
        this.parent.setUnpinned();
      }
    };

    this.findOrAddChild = function (raw) {
      if (!this.children) {
        this.children = [];
      }

      for (let i = 0; i < this.children.length; i++) {
        const child = this.children[i];
        if (this.fg.$$frameEquator(this.fg.$dataSource, child.raw, raw)) {
          return child;
        }
      }

      return this.addChild(raw);
    };

    this.addChild = function (raw) {
      if (!this.children) {
        this.children = [];
      }

      const child = new Frame(this.fg, raw, this.depth + 1);
      child.index = this.children.length;
      child.parent = this;
      this.children.push(child);
      return child;
    };

    this.sort = function () {
      if (!this.children) {
        return;
      }

      if (this.children.length > 1) {
        this.children.sort((left, right) => right.weight - left.weight);
      }

      for (let i = 0; i < this.children.length; i++) {
        this.children[i].index = i;
        this.children[i].sort();
      }
    };

    this.diffPercent = function () {
      let cp = 0;
      if (this.fg.$totalWeightOfBaseline2 > 0) {
        cp = this.weightOfBaseline2 / this.fg.$totalWeightOfBaseline2;
      }

      let bp = 0;
      if (this.fg.$totalWeightOfBaseline1 > 0) {
        bp = this.weightOfBaseline1 / this.fg.$totalWeightOfBaseline1;
      }

      if (bp > 0) {
        let r = (cp - bp) / bp;
        if (r > 1) {
          return 1;
        }
        if (r < -1) {
          return -1;
        }
        return r;
      } else if (cp > 0) {
        return 1;
      } else {
        return 0;
      }
    };

    this.drawSelf = function () {
      if (!this.isRoot) {
        // generate information and text
        this.infomation = this.fg.$$diff
          ? {
              selfWeight: this.selfWeight,
              weight: this.weight,
              totalWeight: this.fg.$totalWeight,

              selfWeightOfBaseline1: this.selfWeightOfBaseline1,
              weightOfBaseline1: this.weightOfBaseline1,
              totalWeightOfBaseline1: this.fg.$totalWeightOfBaseline1,

              selfWeightOfBaseline2: this.selfWeightOfBaseline2,
              weightOfBaseline2: this.weightOfBaseline2,
              totalWeightOfBaseline2: this.fg.$totalWeightOfBaseline2,

              diffPercent: this.diffPercent()
            }
          : {
              selfWeight: this.selfWeight,
              weight: this.weight,
              totalWeight: this.fg.$totalWeight
            };

        this.text = this.fg.$$textGenerator(flameGraph.$dataSource, raw, this.infomation);

        this.infomation.text = this.text;
      }
      if (!this.color) {
        this.color = this.fg.$$colorSelector(this.fg.dataSource, this.raw, this.infomation);
      }

      this.fg.$context.fillStyle = this.color[0];
      this.fg.$context.fillRect(this.x, this.y, this.width, this.height);

      this.visibleText = null;
      if (this.width > this.fg.$showTextWidthThreshold && this.text.length > 0) {
        this.fg.$context.font = this.isRoot ? this.fg.$rootFont : this.fg.$font;
        this.fg.$context.fillStyle = this.color[1];
        this.fg.$context.textBaseline = 'middle';
        let w = this.fg.$context.measureText(this.text).width;
        let leftW = this.width - 2 * this.fg.$textGap;
        if (w <= leftW) {
          this.fg.$context.fillText(
            this.text,
            this.x + this.fg.$textGap,
            this.y + this.height / 2 + 1
          );
          this.visibleText = this.text;
        } else {
          // truncate text and append suffix
          let len = Math.floor(
            (this.text.length * (leftW - this.fg.$context.measureText(this.fg.$moreText).width)) / w
          );
          let text = null;
          for (let i = len; i > 0; i--) {
            text = this.text.substring(0, len) + this.fg.$moreText;
            if (this.fg.$context.measureText(text).width <= leftW) {
              break;
            }
            text = null;
          }
          if (text != null) {
            this.fg.$context.fillText(
              text,
              this.x + this.fg.$textGap,
              this.y + this.height / 2 + 1
            );
          }
          this.visibleText = text;
        }
      }
      this.fg.$stackTraceMaxDrawnDepth = Math.max(this.depth, this.fg.$stackTraceMaxDrawnDepth);
      this.fg.$sibling[this.depth].push(this);
    };

    this.resetPosition = function () {
      this.x = 0;
      this.y = 0;
      this.width = 0;
      this.height = 0;

      if (this.children) {
        this.children.forEach((c) => c.resetPosition());
      }
    };

    this.draw = function (x, y, w, h) {
      this.x = x;
      this.y = y;
      this.fg.$maxY = Math.max(y + h, this.fg.$maxY);
      this.width = w;
      this.height = h;

      this.drawSelf();

      if (this.children) {
        if (this.fg.$pinned && this === this.fg.$pinnedFrame) {
          this.fg.$drawingChildrenOfPinnedFrame = true;
        }
        let xGap = this.fg.$xGap;
        let childY = this.fg.downward ? y + h + this.fg.$yGap : y - h - this.fg.$yGap;
        if (
          !this.fg.$pinned ||
          this === this.fg.$pinnedFrame ||
          this.fg.$drawingChildrenOfPinnedFrame
        ) {
          const space = this.children.length - 1;
          let leftWidth = w;
          if ((space * xGap) / w > this.fg.$xGapThreashold) {
            xGap = 0;
          } else {
            leftWidth = leftWidth - space * xGap;
          }
          let endX = x + w;
          let nextX = x;
          for (let i = 0; i < this.children.length; i++) {
            let cw = 0;
            if (i === this.children.length - 1 && this.selfWeight === 0) {
              cw = endX - nextX;
            } else {
              cw = (leftWidth * this.children[i].weight) / this.weight;
            }
            this.children[i].draw(nextX, childY, cw, h);
            nextX += cw + xGap;
          }
        } else {
          let sideWidth = 15;
          if (this === this.fg.$pinnedFrameLeft || this === this.fg.$pinnedFrameRight) {
            this.fg.$drawingChildrenOfSideFrame = true;
            this.fg.$drawingLeftSide = this === this.fg.$pinnedFrameLeft;
          }
          if (this.fg.$drawingChildrenOfSideFrame) {
            if (!this.fg.$drawingLeftSide || this.selfWeight === 0) {
              for (let i = 0; i < this.children.length; i++) {
                if (
                  (this.fg.$drawingLeftSide && i === this.children.length - 1) ||
                  (!this.fg.$drawingLeftSide && i === 0)
                ) {
                  this.children[i].draw(x, childY, sideWidth, h);
                } else {
                  this.children[i].resetPosition();
                }
              }
            } else {
              for (let i = 0; i < this.children.length; i++) {
                this.children[i].resetPosition();
              }
            }
          } else {
            for (let i = 0; i < this.children.length; i++) {
              let xGap = this.fg.$xGap;
              if ((xGap * 2) / w > this.fg.$xGapThreashold) {
                xGap = 0;
              }
              if (this.children[i].pinned) {
                let cx = x;
                let cw = w;
                if (this.hasLeftSide) {
                  cx += sideWidth + xGap;
                  cw -= sideWidth + xGap;
                }
                if (this.hasRightSide) {
                  cw -= sideWidth + xGap;
                } else if (this.selfWeight > 0 && this.fg.$pinnedFrame.parent === this) {
                  cw -= sideWidth;
                }
                this.children[i].draw(cx, childY, cw, h);
              } else if (this.children[i] === this.fg.$pinnedFrameLeft) {
                this.children[i].draw(x, childY, sideWidth, h);
              } else if (this.children[i] === this.fg.$pinnedFrameRight) {
                this.children[i].draw(x + w - sideWidth, childY, sideWidth, h);
              } else {
                this.children[i].resetPosition();
              }
            }
          }
          if (this === this.fg.$pinnedFrameLeft || this === this.fg.$pinnedFrameRight) {
            this.fg.$drawingChildrenOfSideFrame = false;
          }
        }
        if (this.fg.$pinned && this === this.fg.$pinnedFrame) {
          this.fg.$drawingChildrenOfPinnedFrame = false;
        }
      }
    };

    this.contain = function (x, y) {
      return x > this.x && x < this.x + this.width && y > this.y && y < this.y + this.height;
    };

    this.maxDepth = function () {
      let maxDepth = this.depth;
      if (this.children) {
        for (let i = 0; i < this.children.length; i++) {
          maxDepth = Math.max(maxDepth, this.children[i].maxDepth());
        }
      }
      return maxDepth;
    };

    function hexToRGB(hex, alpha = 1) {
      let r = parseInt(hex.slice(1, 3), 16),
        g = parseInt(hex.slice(3, 5), 16),
        b = parseInt(hex.slice(5, 7), 16);

      if (hex.length === 9) {
        alpha = parseInt(hex.slice(7, 9), 16) / 255;
      }

      return 'rgba(' + r + ', ' + g + ', ' + b + ', ' + alpha + ')';
    }

    // noinspection JSUnusedLocalSymbols
    this.touch = function (x, y) {
      this.fg.$frameMask.style.left = this.x + 'px';
      this.fg.$frameMask.style.top = this.y + 'px';

      this.fg.$frameMask.style.width = this.width + 'px';
      this.fg.$frameMask.style.height = this.height + 'px';
      this.fg.$frameMask.style.backgroundColor = this.color[0];

      this.fg.$frameMaskText.style.color = this.color[1];
      this.fg.$frameMaskText.style.paddingLeft = this.fg.$textGap + 'px';
      this.fg.$frameMaskText.style.lineHeight = this.fg.$frameMask.style.height;
      this.fg.$frameMaskText.style.fontSize = this === this.fg.$root ? '14px' : '12px';
      this.fg.$frameMaskText.innerText = this.visibleText;

      this.fg.$frameMask.style.cursor = 'pointer';
      this.fg.$frameMask.style.visibility = 'visible';
      this.fg.$frameMask.focus();

      let top = this.y + this.height - this.fg.$flameGraphInner.scrollTop;
      let detailsNode = this.fg.shadowRoot.getElementById('frame-postcard-content-main-details');
      if (detailsNode) {
        detailsNode.parentNode.removeChild(detailsNode);
      }

      if (this !== this.fg.$root) {
        this.fg.$framePostcardContentMain.style.backgroundColor = this.color[0];
        this.fg.$framePostcardContentMain.style.color = this.color[1];
        let hp = Math.round((this.depth / this.maxDepth()) * 100);
        let direction = this.fg.downward ? 'to bottom' : 'to top';

        // title
        this.fg.$framePostcardContentMainTitle.innerText = this.fg.$$titleGenerator(
          this.fg.$dataSource,
          this.raw,
          this.infomation
        );
        this.fg.$framePostcardContentMainLine.style.background =
          'linear-gradient(' +
          direction +
          ', ' +
          hexToRGB(this.color[1], 0.7) +
          ' 0% ' +
          hp +
          '%, ' +
          hexToRGB(this.color[1], 0.2) +
          ' ' +
          hp +
          '% 100%)';

        // details
        let details = this.fg.$$detailsGenerator(this.fg.$dataSource, this.raw, this.infomation);
        if (details) {
          let keys = Object.keys(details);
          let content = null;
          if (keys.length > 0) {
            content =
              '<div id ="frame-postcard-content-main-details" style="width: 100%; font-size: 11px; word-wrap: break-word">';
            for (let i = 0; i < keys.length; i++) {
              content += '<div style="margin-top: 5px; opacity: .7">' + keys[i] + '</div>';
              content += '<ul style="margin: 2px 0 0 -15px"><li>' + details[keys[i]] + '</li></ul>';
            }
            content += '</div>';
          }
          if (content != null) {
            let t = document.createElement('template');
            t.innerHTML = content.trim();
            this.fg.$framePostcardContentMain.appendChild(t.content.firstChild);
          }
        }

        // foot
        this.fg.$framePostcardContentFoot.innerText = this.fg.$$footTextGenerator(
          this.fg.$dataSource,
          this.raw,
          this.infomation
        );
        let wp = Math.round((this.weight / this.fg.$totalWeight) * 100);

        let footColor = this.fg.$$footColorSelector(this.fg.$dataSource, this.raw, this.infomation);
        let startColor, endColor, fontColor;
        if (footColor.length > 2) {
          startColor = footColor[0];
          endColor = footColor[1];
          fontColor = footColor[2];
        } else {
          startColor = endColor = footColor[0];
          fontColor = footColor[2];
        }
        this.fg.$framePostcardContentFoot.style.background =
          'linear-gradient(to right, ' +
          hexToRGB(startColor) +
          ' 0% ' +
          wp +
          '%, ' +
          hexToRGB(endColor) +
          ' ' +
          wp +
          '% 100%)';
        this.fg.$framePostcardContentFoot.style.color = fontColor;

        this.fg.$framePostcardShadow.style.left = x + 'px';
        this.fg.$framePostcardShadow.style.top = top + 'px';
        this.fg.$framePostcard.style.visibility = 'visible';
        this.fg.decideFramePostcardLayout();

        if (this.fg.$$diff) {
          let diffPercent = this.diffPercent();
          let top;
          if (diffPercent > 0) {
            top = 0.5 * (1 - diffPercent) * 100 + '%';
          } else {
            top = (0.5 + 0.5 * -diffPercent) * 100 + '%';
          }
          this.fg.$colorArrow.style.top = top;
          this.fg.$colorArrow.style.visibility = 'visible';
        }
      }
      this.fg.$currentFrame = this;
    };

    this.leave = function () {
      this.fg.$framePostcard.style.visibility = 'hidden';
      this.fg.$frameMask.style.visibility = 'hidden';
      this.fg.$currentFrame = null;

      if (this.fg.$$diff) {
        this.fg.$colorArrow.style.visibility = 'hidden';
      }
    };

    // only clear the children and weight since this method only used by root
    this.clear = function () {
      this.children = null;
      this.weight = 0;
    };
  }

  class FlameGraph extends HTMLElement {
    constructor() {
      super();
      this.attachShadow({ mode: 'open' });
      let sr = this.shadowRoot;
      sr.appendChild(template.content.cloneNode(true));

      this.$canvas = sr.getElementById('flame-graph-canvas');
      this.$context = this.$canvas.getContext('2d');
      this.$context.save();

      this.$frameHeight = 24;

      this.$fgHGap = 15;
      this.$fgVGap = 0;
      this.$fgVEndGap = 5;
      this.$xGap = 0.2;
      this.$xGapThreashold = 0.01;
      this.$yGap = 0.5;
      this.$textGap = 6;
      this.$showTextWidthThreshold = 30;
      this.$fontFamily = 'Menlo,NotoSans,"Lucida Grande","Lucida Sans Unicode",sans-serif';
      this.$font = '400 12px ' + this.$fontFamily;
      this.$font_600 = '600 12px ' + this.$fontFamily;
      this.$rootFont = '600 14px ' + this.$fontFamily;
      this.$moreText = '...';

      this.$defaultColorScheme = {
        colorForZero: ['#c5c8d3', '#000000'],
        colors: [
          ['#761d96', '#ffffff'],
          ['#c12561', '#ffffff'],
          ['#fec91b', '#000000'],
          ['#3f7350', '#ffffff'],
          ['#408118', '#ffffff'],
          ['#3ea9da', '#000000'],
          ['#9fb036', '#ffffff'],
          ['#b671c1', '#ffffff'],
          ['#faa938', '#000000']
        ]
      };

      this.$maxY = 0;

      this.$flameGraph = sr.getElementById('flame-graph');
      this.$flameGraphInner = sr.getElementById('flame-graph-inner');
      this.$flameGraphInnerWrapper = sr.getElementById('flame-graph-inner-wrapper');
      this.$pinnedFrameMask = sr.getElementById('pinned-frame-mask');
      this.$frameMask = sr.getElementById('frame-mask');
      this.$flameGraphHelp = sr.getElementById('flame-graph-help');
      this.$closeFlameGraphHelp = sr.getElementById('close-flame-graph-help');

      this.$helpButton = sr.getElementById('help-button');

      this.$helpButton.addEventListener('click', () => {
        if (this.$flameGraphHelp.style.visibility !== 'visible') {
          this.$flameGraphHelp.style.visibility = 'visible';
        } else {
          this.$flameGraphHelp.style.visibility = 'hidden';
        }
      });

      this.$flameGraphInner.addEventListener('click', () => {
        if (this.$flameGraphHelp.style.visibility === 'visible') {
          this.$flameGraphHelp.style.visibility = 'hidden';
        }
      });

      this.$closeFlameGraphHelp.addEventListener('click', () => {
        this.$flameGraphHelp.style.visibility = 'hidden';
      });
      this.$commandMode = false;
      this.$frameMask.addEventListener('keydown', (e) => {
        if ((e.key === 'c' || e.key === 'C') && (e.metaKey || e.ctrlKey)) {
          this.copy(false);
          this.$commandMode = false;
        } else {
          if (this.$commandMode) {
            if (e.key === 'f' || e.key === 'F') {
              this.copy(false);
            } else if (e.key === 's' || e.key === 'S') {
              this.copy(true);
            }
            this.$commandMode = false;
          } else if (e.key === 'f' || e.key === 'F') {
            this.$commandMode = true;
          }
        }
      });
      this.$frameMaskText = sr.getElementById('frame-mask-text');
      this.$framePostcard = sr.getElementById('frame-postcard');
      this.$framePostcardShadow = sr.getElementById('frame-postcard-shadow');
      this.$framePostcardConnectingLine = sr.getElementById('frame-postcard-connecting-line');
      this.$framePostcardContent = sr.getElementById('frame-postcard-content');
      this.$framePostcardContentMain = sr.getElementById('frame-postcard-content-main');
      this.$framePostcardContentMainLine = sr.getElementById('frame-postcard-content-main-line');
      this.$framePostcardContentMainTitle = sr.getElementById('frame-postcard-content-main-title');
      this.$framePostcardContentFoot = sr.getElementById('frame-postcard-content-foot');

      this.$frameMask.style.font = this.$font_600;

      this.$root = null;

      this.$currentFrame = null;
      this.$pinned = false;
      this.$pinnedFrame = null;
      this.$pinnedFrameLeft = null;
      this.$pinnedFrameRight = null;

      this.$drawingChildrenOfPinnedFrame = false;

      this.$frameMask.addEventListener('mousemove', (e) => {
        this.handleFrameMaskMouseMoveEvent(e);
      });
      this.$frameMask.addEventListener('click', (e) => {
        this.handleFrameMaskClickEvent(e);
      });
      this.$frameMask.addEventListener('dblclick', (e) => {
        if (window.getSelection) {
          window.getSelection().removeAllRanges();
        }
        this.handleFrameMaskClickEvent(e);
      });

      this.$scrollEventListener = () => {
        this.handleScrollEvent();
      };

      this.$flameGraphInner.addEventListener('scroll', this.$scrollEventListener);

      window.addEventListener('scroll', this.$scrollEventListener);

      this.$downwardBunnton = sr.getElementById('downward-button');
      this.$downwardBunnton.addEventListener('click', () => (this.downward = !this.downward));

      this.$root = new Frame(this, null, 0, true);

      this.$touchedFrame = null;

      this.$canvas.addEventListener('mousemove', (e) => {
        this.handleCanvasMouseMoveEvent(e);
      });

      this.$flameGraph.addEventListener('mouseleave', () => {
        if (this.$touchedFrame) {
          let tf = this.$touchedFrame;
          this.$touchedFrame = null;
          tf.leave();
        }
      });

      this.$totalWeight = 0;

      let o = this;
      new ResizeObserver(function () {
        o.render(true, false);
      }).observe(this.$flameGraph);

      this.$colorBarDiv = sr.getElementById('color-bar-div');
      this.$colorArrow = sr.getElementById('color-arrow');
    }

    handleScrollEvent() {
      this.$currentFrame = null;
      this.$touchedFrame = null;
      this.$frameMask.style.cursor = 'default';
      this.$frameMask.style.visibility = 'hidden';
      this.$framePostcard.style.visibility = 'hidden';

      if (this.$stackTraceMaxDrawnDepth < this.$stackTraceMaxDepth) {
        if (this.downward) {
          if (this.$flameGraphInner.scrollTop > this.$currentScrollTopLimit) {
            this.$flameGraphInner.scrollTop = this.$currentScrollTopLimit;
          }
        } else {
          if (this.$flameGraphInner.scrollTop < this.$currentScrollTopLimit) {
            this.$flameGraphInner.scrollTop = this.$currentScrollTopLimit;
          }
        }
      }
    }

    handleFrameMaskMouseMoveEvent(e) {
      this.$framePostcardShadow.style.left = this.$frameMask.offsetLeft + e.offsetX + 'px';
      this.decideFramePostcardLayout();
      e.stopPropagation();
    }

    handleFrameMaskClickEvent(e) {
      if (window.getSelection().type === 'Range') {
        e.stopPropagation();
        return;
      }

      if (this.$currentFrame === this.$root) {
        return;
      }

      if (!this.$pinned) {
        this.$pinned = true;
        this.$pinnedFrame = this.$currentFrame;
        this.$pinnedFrame.setPinned();
        this.$pinnedFrame.findSide();
      } else {
        if (this.$pinnedFrame === this.$currentFrame) {
          this.$pinnedFrame.setUnpinned();
          this.$pinnedFrame.clearFindSide();
          this.$pinnedFrame = null;
          this.$pinnedFrameMask.style.visibility = 'hidden';
          this.$pinned = false;
        } else {
          this.$pinnedFrame.setUnpinned();
          this.$pinnedFrame.clearFindSide();
          this.$pinnedFrame = this.$currentFrame;
          this.$pinnedFrame.setPinned();
          this.$pinnedFrame.findSide();
        }
      }

      this.render(false, false);

      if (this.$pinned && this.$pinnedFrame) {
        this.$pinnedFrameMask.style.left = this.$pinnedFrame.x + 'px';
        this.$pinnedFrameMask.style.top = this.$pinnedFrame.y + 'px';
        this.$pinnedFrameMask.style.width = this.$pinnedFrame.width + 'px';
        this.$pinnedFrameMask.style.height = this.$pinnedFrame.height + 'px';
        this.$pinnedFrameMask.style.visibility = 'visible';
      }

      let ne = new Event('mousemove');
      ne.offsetX = this.$frameMask.offsetLeft + e.offsetX;
      ne.offsetY = this.$frameMask.offsetTop + e.offsetY;

      this.$framePostcard.style.visibility = 'hidden';
      this.$frameMask.style.cursor = 'default';
      this.$frameMask.style.visibility = 'hidden';

      this.$canvas.dispatchEvent(ne);
      e.stopPropagation();
    }

    render(reInitRenderContext, reGenFrames) {
      {
        // cache: from dataSource
        if (this.$dataSource) {
          this.$$isLineFormat = this.$dataSource.format.toLowerCase() === 'line';
          this.$$diff = !!this.$dataSource.diff;

          // cache: from configuration
          this.$$dataExtractor = this.dataExtractor;

          this.$$stackTracesCounter = this.stackTracesCounter;
          this.$$stackTraceExtractor = this.stackTraceExtractor;
          this.$$framesCounter = this.framesCounter;
          this.$$frameExtractor = this.frameExtractor;
          this.$$framesIndexer = this.framesIndexer;
          this.$$stackTraceFilter = this.stackTraceFilter;
          this.$$frameEquator = this.frameEquator;
          this.$$reverse = this.reverse;

          this.$$rootFramesCounter = this.rootFramesCounter;
          this.$$rootFrameExtractor = this.rootFrameExtractor;
          this.$$childFramesCounter = this.childFramesCounter;
          this.$$childFrameExtractor = this.childFrameExtractor;
          this.$$frameStepper = this.frameStepper;
          this.$$childFramesIndexer = this.childFramesIndexer;

          this.$$weightsExtractor = this.weightsExtractor;

          this.$$rootTextGenerator = this.rootTextGenerator;
          this.$$textGenerator = this.textGenerator;
          this.$$titleGenerator = this.titleGenerator;
          this.$$detailsGenerator = this.detailsGenerator;
          this.$$footTextGenerator = this.footTextGenerator;

          this.$$rootColorSelector = this.rootColorSelector;
          this.$$colorSelector = this.colorSelector;
          this.$$footColorSelector = this.footColorSelector;
          this.$$hashCodeGenerator = this.hashCodeGenerator;

          this.$$showHelpButton = this.showHelpButton;
        }
      }

      if (reInitRenderContext) {
        this.clearState();
      }

      this.clearCanvas();

      if (reGenFrames) {
        this.genFrames();
      }

      if (reInitRenderContext) {
        this.initRenderContext();
      }

      if (this.$totalWeight === 0) {
        this.$helpButton.style.visibility = 'hidden';
        return;
      }

      if (this.$$diff) {
        this.$colorBarDiv.style.display = 'flex';
      }

      this.$helpButton.style.visibility = this.$$showHelpButton ? 'visible' : 'hidden';
      let rect = this.$canvas.getBoundingClientRect();

      this.$stackTraceMaxDrawnDepth = 0;
      this.$sibling = Array(this.$stackTraceMaxDepth + 1);
      for (let i = 0; i < this.$stackTraceMaxDepth + 1; i++) {
        this.$sibling[i] = [];
      }
      if (this.downward) {
        this.$root.draw(
          this.$fgHGap,
          this.$fgVGap,
          rect.width - this.$fgHGap * 2,
          this.$frameHeight
        );
      } else {
        this.$root.draw(
          this.$fgHGap,
          rect.height - this.$fgVGap - this.$frameHeight,
          rect.width - this.$fgHGap * 2,
          this.$frameHeight
        );
      }

      if (this.$stackTraceMaxDrawnDepth < this.$stackTraceMaxDepth) {
        let height = (this.$stackTraceMaxDrawnDepth + 1) * this.$frameHeight;

        if (this.$stackTraceMaxDrawnDepth > 0) {
          height += this.$stackTraceMaxDrawnDepth * this.$yGap;
        }
        height += this.$fgVGap + this.$fgVEndGap;

        if (this.downward) {
          this.$currentScrollTopLimit = Math.max(
            height - this.$flameGraphInner.getBoundingClientRect().height,
            this.$flameGraphInner.scrollTop
          );
        } else {
          this.$currentScrollTopLimit = Math.min(
            this.$flameGraphHeight - height,
            this.$flameGraphInner.scrollTop
          );
        }
      }
    }

    clearState() {
      this.$pinnedFrame = null;
      this.$currentFrame = null;
      this.$touchedFrame = null;
      this.$pinnedFrameMask.style.visibility = 'hidden';
      this.$frameMask.style.cursor = 'default';
      this.$frameMask.style.visibility = 'hidden';
      this.$framePostcard.style.visibility = 'hidden';
      this.$pinned = false;
    }

    clearCanvas() {
      this.$context.clearRect(0, 0, this.$canvas.width, this.$canvas.height);
    }

    genFramesFromLineData() {
      let dataSource = this.$dataSource;

      for (let i = 0; i < this.$$stackTracesCounter(dataSource); i++) {
        const stackTrace = this.$$stackTraceExtractor(dataSource, i);
        if (!this.$$stackTraceFilter(dataSource, stackTrace)) {
          continue;
        }

        const frameCount = this.$$framesCounter(dataSource, stackTrace);

        if (frameCount === 0) {
          return;
        }

        this.$stackTraceMaxDepth = Math.max(this.$stackTraceMaxDepth, frameCount);

        let weights = this.$$weightsExtractor(dataSource, stackTrace);
        let weight, weightOfBaseline1, weightOfBaseline2;
        if (this.$$diff) {
          [weightOfBaseline1, weightOfBaseline2] = weights;
          weight = weightOfBaseline1 + weightOfBaseline2;
          this.$totalWeightOfBaseline1 += weightOfBaseline1;
          this.$totalWeightOfBaseline2 += weightOfBaseline2;
        } else {
          weight = weights;
        }

        this.$totalWeight += weight;
        this.$root.addWeight(weight);

        let current = this.$root;
        let j = this.$$reverse ? frameCount - 1 : 0;
        let end = this.$$reverse ? -1 : frameCount;
        let step = this.$$reverse ? -1 : 1;
        for (; j !== end; j += step) {
          const frame = this.$$frameExtractor(dataSource, stackTrace, j);
          const child = current.findOrAddChild(frame);
          child.addWeight(weight);
          if (this.$$diff) {
            child.addWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);
          }
          current = child;
        }
        current.addSelfWeight(weight);
        if (this.$$diff) {
          current.addSelfWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);
        }
      }
    }

    genFramesFromTreeData() {
      const queue = [];
      let dataSource = this.$dataSource;

      const process = (parent, frame) => {
        let child = parent.addChild(frame);

        let weights = this.$$weightsExtractor(dataSource, frame);
        let selfWeight,
          weight,
          selfWeightOfBaseline1,
          weightOfBaseline1,
          selfWeightOfBaseline2,
          weightOfBaseline2;
        if (this.$$diff) {
          [selfWeightOfBaseline1, weightOfBaseline1, selfWeightOfBaseline2, weightOfBaseline2] =
            weights;

          child.addSelfWeightOfBaselines(selfWeightOfBaseline1, selfWeightOfBaseline2);
          child.addWeightOfBaselines(weightOfBaseline1, weightOfBaseline2);

          selfWeight = selfWeightOfBaseline1 + selfWeightOfBaseline2;
          weight = weightOfBaseline1 + weightOfBaseline2;
        } else {
          [selfWeight, weight] = weights;
        }

        child.addSelfWeight(selfWeight);
        child.addWeight(weight);

        this.$stackTraceMaxDepth = Math.max(this.$stackTraceMaxDepth, child.depth);

        if (this.$$childFramesCounter(dataSource, frame) > 0) {
          queue.push(child);
        }
        return child;
      };

      const rootFramesCount = this.$$rootFramesCounter(dataSource);
      for (let i = 0; i < rootFramesCount; i++) {
        const rootFrame = process(this.$root, this.$$rootFrameExtractor(dataSource, i));
        this.$totalWeight += rootFrame.weight;
        if (this.$$diff) {
          this.$totalWeightOfBaseline1 += rootFrame.weightOfBaseline1;
          this.$totalWeightOfBaseline2 += rootFrame.weightOfBaseline2;
        }
      }

      this.$root.addWeight(this.$totalWeight);

      while (queue.length > 0) {
        const frame = queue.shift();
        const childrenCount = this.$$childFramesCounter(dataSource, frame.raw);
        for (let i = 0; i < childrenCount; i++) {
          process(frame, this.$$childFrameExtractor(dataSource, frame.raw, i));
        }
      }
    }

    genFrames() {
      this.$root.clear();
      this.$stackTraceMaxDepth = 0;
      this.$totalWeight = 0;
      this.$totalWeightOfBaseline1 = 0;
      this.$totalWeightOfBaseline2 = 0;

      if (this.$dataSource) {
        let format = this.$dataSource.format;
        if (format === 'line') {
          this.genFramesFromLineData();
        } else if (format === 'tree') {
          if (this.$$reverse) {
            console.warn("Tree format data doesn't support reverse");
          }
          this.genFramesFromTreeData();
        } else {
          throw new Error(`Unsupported dataSource format ${format}`);
        }
      }

      this.$root.sort();

      this.$information = this.$$diff
        ? {
            totalWeight: this.$totalWeight,
            totalWeightOfBaseline1: this.$totalWeightOfBaseline1,
            totalWeightOfBaseline2: this.$totalWeightOfBaseline2
          }
        : {
            totalWeight: this.$totalWeight
          };

      this.$root.text = this.$$rootTextGenerator(this.$dataSource, this.$information);
    }

    initRenderContext() {
      let w = this.width;
      if (w) {
        if (w.endsWith('%')) {
          this.$flameGraph.style.width = w;
        } else {
          this.$flameGraph.style.width = w + 'px';
        }
      }

      let h = this.height;
      if (h) {
        if (h.endsWith('%')) {
          this.$flameGraph.style.height = h;
        } else {
          this.$flameGraph.style.height = h + 'px';
        }
      }

      this.$context.restore();
      this.$context.save();

      let height = (this.$stackTraceMaxDepth + 1) * this.$frameHeight;

      if (this.$stackTraceMaxDepth > 0) {
        height += this.$stackTraceMaxDepth * this.$yGap;
      }
      height += this.$fgVGap + this.$fgVEndGap;

      this.$flameGraphInnerWrapper.style.height = height + 'px';
      this.$flameGraphHeight = height;

      let innerHeight = this.$flameGraphInner.getBoundingClientRect().height;
      this.$flameGraphInner.style.overflowY = null;
      if (innerHeight < height) {
        this.$flameGraphInner.style.overflowY = 'auto';
      } else if (!this.downward) {
        this.$flameGraphInnerWrapper.style.height = innerHeight + 'px';
      }

      if (!this.downward) {
        this.$flameGraphInner.scrollTop = this.$flameGraphInner.scrollHeight;
        this.$downwardBunnton.style.background = 'grey';
        this.$downwardBunnton.style.flexDirection = 'row';
      } else {
        this.$flameGraphInner.scrollTop = 0;
        this.$downwardBunnton.style.background = 'rgb(24, 144, 255)';
        this.$downwardBunnton.style.flexDirection = 'row-reverse';
      }

      const dpr = window.devicePixelRatio || 1;
      const rect = this.$canvas.getBoundingClientRect();
      this.$canvas.width = rect.width * dpr;
      this.$canvas.height = rect.height * dpr;
      this.$context.scale(dpr, dpr);

      if (this.$dataSource) {
        this.$root.color = this.$$rootColorSelector(this.$dataSource, this.$information);
      }
      this.$colorBarDiv.style.display = 'none';
    }

    connectedCallback() {
      this.addEventListener('re-render', () => {
        this.render(true, true);
      });
    }

    disconnectedCallback() {
      window.removeEventListener('scroll', this.$scrollEventListener);
    }

    get width() {
      return this.getAttribute('width');
    }

    set width(w) {
      this.setAttribute('width', w);
    }

    get height() {
      return this.getAttribute('height');
    }

    set height(h) {
      this.setAttribute('height', h);
    }

    get downward() {
      return this.hasAttribute('downward');
    }

    set downward(downward) {
      this.toggleAttribute('downward', !!downward);
    }

    static get observedAttributes() {
      return ['width', 'height', 'downward'];
    }

    attributeChangedCallback(name, oldVal, newVal) {
      if (!this.$dataSource || oldVal === newVal) {
        return;
      }
      this.render(true, false);
    }

    set dataSource(dataSource) {
      if (!dataSource.format) {
        throw new Error("Should specify the format of dataSource: 'line' or 'tree'");
      }
      if (typeof dataSource.format !== 'string') {
        throw new Error('Illegal dataSource format type, must be string');
      }
      let format = dataSource.format.toLowerCase();
      if ('line' !== format && 'tree' !== format) {
        throw new Error("Illegal dataSource format, must be 'line' or 'tree'");
      }
      this.$dataSource = dataSource;
      this.render(true, true);
    }

    get dataSource() {
      return this.$dataSource;
    }

    set configuration(configuration) {
      if (typeof configuration !== 'object') {
        throw new Error('Configuration should be an object');
      }
      this.$configuration = configuration;
    }

    get configuration() {
      return this.$configuration;
    }

    getConfigItemOrDefault(name, def) {
      if (this.$configuration && this.$configuration[name]) {
        return this.$configuration[name];
      }
      return def;
    }

    _(name, def) {
      return this.getConfigItemOrDefault(name, def);
    }

    get dataExtractor() {
      return this._('dataExtractor', (dataSource) => dataSource.data);
    }

    get stackTracesCounter() {
      return this._('stackTracesCounter', (dataSource) => this.$$dataExtractor(dataSource).length);
    }

    get stackTraceExtractor() {
      return this._(
        'stackTraceExtractor',
        (dataSource, index) => this.$$dataExtractor(dataSource)[index]
      );
    }

    get framesCounter() {
      return this._('framesCounter', (dataSource, stackTrace) => {
        return stackTrace[this.$$framesIndexer(dataSource, stackTrace)].length;
      });
    }

    get frameExtractor() {
      return this._('frameExtractor', (dataSource, stackTrace, index) => {
        return stackTrace[this.$$framesIndexer(dataSource, stackTrace)][index];
      });
    }

    get framesIndexer() {
      return this._('framesIndexer', (dataSource, stackTrace) => 0);
    }

    get stackTraceFilter() {
      return this._('stackTraceFilter', (dataSource, stackTrace) => true);
    }

    get frameEquator() {
      return this._('frameEquator', (dataSource, left, right) => {
        return left === right;
      });
    }

    get reverse() {
      return !!this._('reverse', false);
    }

    get rootFramesCounter() {
      return this._(
        'rootFramesCounter',
        (dataSource) =>
          this.$$dataExtractor(dataSource).length / this.$$frameStepper(dataSource, null)
      );
    }

    get rootFrameExtractor() {
      return this._('rootFrameExtractor', (dataSource, index) => {
        let steps = this.$$frameStepper(dataSource, null);
        const start = index * steps;
        return this.$$dataExtractor(dataSource).slice(start, start + steps);
      });
    }

    get childFramesCounter() {
      return this._('childFramesCounter', (dataSource, frame) => {
        return (
          frame[this.$$childFramesIndexer(dataSource, frame)].length /
          this.$$frameStepper(dataSource, frame)
        );
      });
    }

    get childFrameExtractor() {
      return this._('childFrameExtractor', (dataSource, frame, index) => {
        let steps = this.$$frameStepper(dataSource, frame);
        const start = index * steps;
        return frame[this.$$childFramesIndexer(dataSource, frame)].slice(start, start + steps);
      });
    }

    get frameStepper() {
      return this._(
        'frameStepper',
        this.$$diff ? (dataSource, frame) => 6 : (dataSource, frame) => 4
      );
    }

    get childFramesIndexer() {
      return this._(
        'childFramesIndexer',
        this.$$diff ? (dataSource, frame) => 5 : (dataSource, frame) => 3
      );
    }

    get weightsExtractor() {
      return this._(
        'weightsExtractor',
        this.$$isLineFormat
          ? this.$$diff
            ? // 1 -> weightOfBaseline1
              // 2 -> weightOfBaseline2
              (dataSource, input) => [input[1], input[2]]
            : // 1 -> weight
              (dataSource, input) => input[1]
          : this.$$diff
          ? // 1 -> selfWeightOfBaseline1
            // 2 -> weightOfBaseline1
            // 3 -> selfWeightOfBaseline2
            // 4 -> weightOfBaseline2
            (dataSource, input) => [input[1], input[2], input[3], input[4]]
          : // 1 -> selfWeight
            // 2 -> weight
            (dataSource, input) => [input[1], input[2]]
      );
    }

    get rootTextGenerator() {
      return this._('rootTextGenerator', (dataSource, information) => {
        let totalWeight = information.totalWeight.toLocaleString();
        if (this.$$diff) {
          let totalWeightOfBaseline1 = information.totalWeightOfBaseline1.toLocaleString();
          let totalWeightOfBaseline2 = information.totalWeightOfBaseline2.toLocaleString();
          return `Total: ${totalWeight} (Baseline1: ${totalWeightOfBaseline1}, Baseline2: ${totalWeightOfBaseline2})`;
        }
        return `Total: ${totalWeight}`;
      });
    }

    get textGenerator() {
      return this._(
        'textGenerator',
        this.$$isLineFormat
          ? (dataSource, frame, information) => frame
          : (dataSource, frame, information) => frame[0]
      );
    }

    get titleGenerator() {
      return this._('titleGenerator', (dataSource, frame, information) => information.text);
    }

    get detailsGenerator() {
      return this._('detailsGenerator', (dataSource, frame, information) => null);
    }

    get footTextGenerator() {
      return this._('footTextGenerator', (dataSource, frame, information) => {
        let selfWeight = information.selfWeight;
        let weight = information.weight;
        let totalWeight = information.totalWeight;
        let value = Math.round((weight / totalWeight) * 100 * 100) / 100;
        return `${value.toLocaleString()}% - (${selfWeight.toLocaleString()}, ${weight.toLocaleString()}, ${totalWeight.toLocaleString()})`;
      });
    }

    get rootColorSelector() {
      return this._('rootColorSelector', (dataSource, information) => ['#537e8b', '#ffffff']);
    }

    get colorSelector() {
      return this._('colorSelector', (dataSource, frame, information) => {
        if (this.$$diff) {
          return [this.diffColor(information.diffPercent), '#ffffff'];
        }
        let hashCode = this.$$hashCodeGenerator(dataSource, frame, information);
        if (hashCode === 0) {
          return this.$defaultColorScheme.colorForZero;
        }
        let colorIndex = Math.abs(hashCode) % this.$defaultColorScheme.colors.length;
        if (!colorIndex && colorIndex !== 0) {
          colorIndex = 0;
        }
        return this.$defaultColorScheme.colors[colorIndex];
      });
    }

    get footColorSelector() {
      return this._('footColorSelector', (dataSource, frame, information) => {
        return ['#537e8bff', '#373b46e6', '#ffffff'];
      });
    }

    get hashCodeGenerator() {
      return this._('hashCodeGenerator', (dataSource, frame, information) => {
        let text = information.text;
        let hash = 0;
        for (let i = 0; i < text.length; i++) {
          hash = 31 * hash + (text.charCodeAt(i) & 0xff);
          hash &= 0xffffffff;
        }
        return hash;
      });
    }

    get showHelpButton() {
      return !!this._('showHelpButton', false);
    }

    hexColorToFloatColor(hex) {
      return [
        parseInt(hex.substring(1, 3), 16) / 255,
        parseInt(hex.substring(3, 5), 16) / 255,
        parseInt(hex.substring(5, 7), 16) / 255
      ];
    }

    floatToHex(f) {
      let v = Math.round(f);
      let r = v.toString(16);
      if (r.length === 1) {
        return '0' + r;
      }
      return r;
    }

    floatColorToHexColor(float) {
      return (
        '#' +
        this.floatToHex(float[0] * 255) +
        this.floatToHex(float[1] * 255) +
        this.floatToHex(float[2] * 255)
      );
    }

    linearColor(from, to, pct) {
      return [
        from[0] + (to[0] - from[0]) * pct,
        from[1] + (to[1] - from[1]) * pct,
        from[2] + (to[2] - from[2]) * pct
      ];
    }

    diffColor(diffPercent) {
      let from = '#808080';
      let to = '#FF0000';
      if (diffPercent < 0) {
        to = '#008000';
        if (diffPercent < -1) {
          diffPercent = -1;
        }
      } else if (diffPercent > 1) {
        diffPercent = 1;
      }

      from = this.hexColorToFloatColor(from);
      to = this.hexColorToFloatColor(to);
      return this.floatColorToHexColor(this.linearColor(from, to, Math.abs(diffPercent)));
    }

    findFrame(x, y) {
      if (!this.$sibling) {
        return null;
      }

      let index;
      if (this.downward) {
        if (y <= this.$root.y) {
          return null;
        }
        index = Math.floor((y - this.$root.y) / (this.$frameHeight + this.$yGap));
      } else {
        if (y >= this.$root.y + this.$frameHeight) {
          return null;
        }
        index = Math.floor(
          (this.$root.y + this.$frameHeight - y) / (this.$frameHeight + this.$yGap)
        );
      }

      if (index >= this.$sibling.length || this.$sibling[index].length === 0) {
        return null;
      }

      let frame = this.$sibling[index][0];
      if (y <= frame.y || y >= frame.y + frame.height) {
        return null;
      }

      let start = 0;
      let end = this.$sibling[index].length - 1;
      while (start <= end) {
        const mid = (start + end) >>> 1;
        frame = this.$sibling[index][mid];
        if (x <= frame.x) {
          end = mid - 1;
        } else if (x >= frame.x + frame.width) {
          start = mid + 1;
        } else {
          return frame;
        }
      }
      return null;
    }

    handleCanvasMouseMoveEvent(e) {
      let lastTouchedFrame = this.$touchedFrame;

      if (lastTouchedFrame) {
        if (lastTouchedFrame.contain(e.offsetX, e.offsetY)) {
          lastTouchedFrame.touch(e.offsetX, e.offsetY);
          return;
        }
      }

      this.$touchedFrame = this.findFrame(e.offsetX, e.offsetY);

      if (lastTouchedFrame !== null && lastTouchedFrame !== this.$touchedFrame) {
        lastTouchedFrame.leave();
      }

      if (this.$touchedFrame) {
        this.$touchedFrame.touch(e.offsetX, e.offsetY);
      }
      e.stopPropagation();
    }

    decideFramePostcardLayout() {
      let rect = this.$framePostcardShadow.getBoundingClientRect();

      this.$framePostcard.style.left = rect.left + 'px';
      this.$framePostcard.style.top = rect.top + 'px';

      let height = this.$framePostcardContent.getBoundingClientRect().height + 26;

      let showAtTop = rect.top - height < 0;
      if (showAtTop) {
        this.$framePostcardContent.style.top = '26px';
        this.$framePostcardContent.style.bottom = null;
      } else {
        this.$framePostcardContent.style.top = null;
        this.$framePostcardContent.style.bottom = '26px';
      }
      let showAtLeft =
        rect.left + 392 > (window.innerWidth || document.documentElement.clientWidth);
      if (showAtLeft) {
        this.$framePostcardContentMain.style.marginLeft =
          366 - this.$framePostcardContentMain.clientWidth + 'px';
        this.$framePostcardContent.style.left = '-392px';
        if (showAtTop) {
          this.$framePostcardConnectingLine.style.transform =
            'rotate(135deg) translate3d(0px, -.5px, 0)';
        } else {
          this.$framePostcardConnectingLine.style.transform =
            'rotate(-135deg) translate3d(0px, -.5px, 0)';
        }
      } else {
        this.$framePostcardContentMain.style.marginLeft = '0px';
        this.$framePostcardContent.style.left = '26px';
        if (showAtTop) {
          this.$framePostcardConnectingLine.style.transform =
            'rotate(45deg) translate3d(0px, -.5px, 0)';
        } else {
          this.$framePostcardConnectingLine.style.transform =
            'rotate(-45deg) translate3d(0px, -.5px, 0)';
        }
      }
    }

    copy(stackTrace) {
      let text = this.$touchedFrame.text;
      if (stackTrace) {
        let f = this.$touchedFrame.parent;
        while (f && f !== this.$root) {
          text += '\n' + f.text;
          f = f.parent;
        }
      }
      if (navigator.clipboard && window.isSecureContext && false) {
        navigator.clipboard.writeText(text).then(() => {
          this.dispatchEvent(
            new CustomEvent('copied', {
              detail: {
                text: text
              }
            })
          );
        });
      } else {
        let textArea = document.createElement('textarea');
        textArea.value = text;
        textArea.style.position = 'fixed';
        textArea.style.left = '-999999px';
        textArea.style.top = '-999999px';
        document.body.appendChild(textArea);
        textArea.focus();
        textArea.select();
        // noinspection JSDeprecatedSymbols
        let success = document.execCommand('copy');
        textArea.remove();
        // re-focus
        this.$frameMask.focus();
        if (success) {
          this.dispatchEvent(
            new CustomEvent('copied', {
              detail: {
                text: text
              }
            })
          );
        }
      }
    }
  }

  window.customElements.define('flame-graph', FlameGraph);
}
