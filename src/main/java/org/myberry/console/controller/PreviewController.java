/*
* MIT License
*
* Copyright (c) 2021 MyBerry. All rights reserved.
* https://myberry.org/
*
* Permission is hereby granted, free of charge, to any person obtaining a copy
* of this software and associated documentation files (the "Software"), to deal
* in the Software without restriction, including without limitation the rights
* to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
* copies of the Software, and to permit persons to whom the Software is
* furnished to do so, subject to the following conditions:

*   * Redistributions of source code must retain the above copyright notice, this
* list of conditions and the following disclaimer.

*   * Redistributions in binary form must reproduce the above copyright notice,
* this list of conditions and the following disclaimer in the documentation
* and/or other materials provided with the distribution.

*   * Neither the name of MyBerry. nor the names of its contributors may be used
* to endorse or promote products derived from this software without specific
* prior written permission.

* The above copyright notice and this permission notice shall be included in all
* copies or substantial portions of the Software.

* THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
* IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
* FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
* AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
* LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
* OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
* SOFTWARE.
*/
package org.myberry.console.controller;

import javax.annotation.Resource;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.myberry.console.annotation.ResponseData;
import org.myberry.console.service.PreviewService;
import org.myberry.console.vo.ComponentVO;
import org.myberry.console.vo.ConsoleVO;
import org.myberry.console.vo.NodeVO;
import org.myberry.console.vo.WeightVO;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.ModelAndView;

@RestController
public class PreviewController {

  @Qualifier("previewService")
  @Resource
  private PreviewService previewService;

  @GetMapping("/")
  public ModelAndView console(ModelAndView mv) throws Exception {
    ConsoleVO consoleVO = previewService.console();

    mv.addObject("consoleAll", consoleVO);
    mv.setViewName("index");
    return mv;
  }

  @PostMapping("/component/search")
  @ResponseBody
  @ResponseData
  public Object componentSearch(@RequestBody ComponentVO componentReq) throws Exception {
    return previewService.queryComponentByKey(componentReq.getKey());
  }

  @PostMapping("/component/create")
  @ResponseBody
  @ResponseData
  public Object componentCreate(@RequestBody ComponentVO componentReq) throws Exception {
    return previewService.componentCreate(componentReq);
  }

  @PostMapping("/weight/update")
  @ResponseBody
  @ResponseData
  public Object weightUpdate(@RequestBody WeightVO weightReq) throws Exception {
    return previewService.weightUpdate(weightReq);
  }

  @PostMapping("/block/query")
  @ResponseBody
  @ResponseData
  public Object blockQuery(@RequestBody NodeVO nodeReq) throws Exception {
    return previewService.blockQuery(nodeReq.getSid());
  }

  @PostMapping("/node/kickOut")
  public void nodeKickOut(
      @RequestBody NodeVO nodeReq, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    previewService.nodeKickOut(nodeReq.getSid());

    if (isAjax(request)) {
      response.setHeader("redirect", "redirect");
      response.setHeader("redirectUrl", "/");
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  @PostMapping("/node/remove")
  public void nodeRemove(
      @RequestBody NodeVO nodeReq, HttpServletRequest request, HttpServletResponse response)
      throws Exception {
    previewService.nodeRemove(nodeReq.getSid());

    if (isAjax(request)) {
      response.setHeader("redirect", "redirect");
      response.setHeader("redirectUrl", "/");
      response.setStatus(HttpServletResponse.SC_OK);
    }
  }

  private boolean isAjax(HttpServletRequest request) {
    return "XMLHttpRequest".equals(request.getHeader("X-Requested-With"));
  }
}
