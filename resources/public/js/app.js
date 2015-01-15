goog.addDependency("base.js", ['goog'], []);
goog.addDependency("../cljs/core.js", ['cljs.core'], ['goog.string', 'goog.object', 'goog.string.StringBuffer', 'goog.array']);
goog.addDependency("../clojure/browser/event.js", ['clojure.browser.event'], ['cljs.core', 'goog.events.EventTarget', 'goog.events.EventType', 'goog.events']);
goog.addDependency("../clojure/browser/net.js", ['clojure.browser.net'], ['goog.net.xpc.CfgFields', 'goog.net.XhrIo', 'goog.json', 'goog.Uri', 'cljs.core', 'goog.net.EventType', 'goog.net.xpc.CrossPageChannel', 'clojure.browser.event']);
goog.addDependency("../weasel/impls/websocket.js", ['weasel.impls.websocket'], ['cljs.core', 'clojure.browser.net', 'goog.net.WebSocket', 'clojure.browser.event']);
goog.addDependency("../cljs/reader.js", ['cljs.reader'], ['goog.string', 'cljs.core', 'goog.string.StringBuffer']);
goog.addDependency("../weasel/repl.js", ['weasel.repl'], ['weasel.impls.websocket', 'cljs.core', 'clojure.browser.net', 'cljs.reader', 'clojure.browser.event']);
goog.addDependency("../contrast/pixel.js", ['contrast.pixel'], ['cljs.core']);
goog.addDependency("../om/dom.js", ['om.dom'], ['cljs.core', 'goog.object']);
goog.addDependency("../cljs/core/async/impl/protocols.js", ['cljs.core.async.impl.protocols'], ['cljs.core']);
goog.addDependency("../cljs/core/async/impl/buffers.js", ['cljs.core.async.impl.buffers'], ['cljs.core', 'cljs.core.async.impl.protocols']);
goog.addDependency("../cljs/core/async/impl/dispatch.js", ['cljs.core.async.impl.dispatch'], ['cljs.core', 'cljs.core.async.impl.buffers', 'goog.async.nextTick']);
goog.addDependency("../cljs/core/async/impl/channels.js", ['cljs.core.async.impl.channels'], ['cljs.core.async.impl.dispatch', 'cljs.core', 'cljs.core.async.impl.buffers', 'cljs.core.async.impl.protocols']);
goog.addDependency("../cljs/core/async/impl/ioc_helpers.js", ['cljs.core.async.impl.ioc_helpers'], ['cljs.core', 'cljs.core.async.impl.protocols']);
goog.addDependency("../cljs/core/async/impl/timers.js", ['cljs.core.async.impl.timers'], ['cljs.core.async.impl.channels', 'cljs.core.async.impl.dispatch', 'cljs.core', 'cljs.core.async.impl.protocols']);
goog.addDependency("../cljs/core/async.js", ['cljs.core.async'], ['cljs.core.async.impl.channels', 'cljs.core.async.impl.dispatch', 'cljs.core', 'cljs.core.async.impl.buffers', 'cljs.core.async.impl.protocols', 'cljs.core.async.impl.ioc_helpers', 'cljs.core.async.impl.timers']);
goog.addDependency("../om/core.js", ['om.core'], ['goog.dom', 'cljs.core', 'om.dom', 'goog.ui.IdGenerator']);
goog.addDependency("../contrast/canvas.js", ['contrast.canvas'], ['contrast.pixel', 'cljs.core', 'om.dom', 'cljs.core.async', 'om.core']);
goog.addDependency("../contrast/common.js", ['contrast.common'], ['cljs.core', 'om.dom']);
goog.addDependency("../contrast/dom.js", ['contrast.dom'], ['cljs.core']);
goog.addDependency("../contrast/slider.js", ['contrast.slider'], ['goog.string', 'cljs.core', 'contrast.common', 'om.dom', 'contrast.dom', 'cljs.core.async', 'goog.string.format', 'om.core']);
goog.addDependency("../contrast/row_probe.js", ['contrast.row_probe'], ['cljs.core', 'contrast.common', 'om.dom', 'contrast.dom', 'om.core']);
goog.addDependency("../contrast/layeredcanvas.js", ['contrast.layeredcanvas'], ['contrast.canvas', 'contrast.pixel', 'cljs.core', 'om.dom', 'cljs.core.async', 'om.core']);
goog.addDependency("../contrast/illusions.js", ['contrast.illusions'], ['contrast.canvas', 'contrast.slider', 'contrast.layeredcanvas', 'cljs.core', 'om.dom', 'cljs.core.async', 'om.core']);
goog.addDependency("../contrast/pixel_probe.js", ['contrast.pixel_probe'], ['contrast.canvas', 'contrast.pixel', 'contrast.slider', 'contrast.layeredcanvas', 'cljs.core', 'om.dom', 'contrast.dom', 'cljs.core.async', 'om.core', 'contrast.illusions']);
goog.addDependency("../contrast/core.js", ['contrast.core'], ['contrast.canvas', 'contrast.slider', 'contrast.row_probe', 'contrast.layeredcanvas', 'cljs.core', 'om.dom', 'contrast.dom', 'cljs.core.async', 'om.core', 'contrast.pixel_probe', 'contrast.illusions']);
goog.addDependency("../clojure/string.js", ['clojure.string'], ['goog.string', 'cljs.core', 'goog.string.StringBuffer']);
goog.addDependency("../figwheel/client.js", ['figwheel.client'], ['goog.net.jsloader', 'cljs.core', 'cljs.core.async', 'clojure.string', 'cljs.reader']);
goog.addDependency("../contrast/dev.js", ['contrast.dev'], ['weasel.repl', 'contrast.core', 'cljs.core', 'cljs.core.async', 'figwheel.client']);
goog.addDependency("../contrast/playground.js", ['contrast.playground'], ['contrast.canvas', 'contrast.slider', 'contrast.layeredcanvas', 'cljs.core', 'om.dom', 'cljs.core.async', 'om.core']);