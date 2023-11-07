/**
 * Kplayer播放器内置功能
 */
var tReq;
var tTimer;
var pingInt;
$(function() {
	window.onresize = function() {
		showCloseBtn();
	}
	pingInt = setInterval("ping()", 60000);
	var fileId = getFileId();
	$
		.ajax({
			url: 'homeController/playVideo.ajax',
			type: 'POST',
			dataType: 'text',
			data: {
				fileId: fileId
			},
			success: function(result) {
				if (result != "ERROR") {
					f = eval("(" + result + ")");
					$("#vname").text(f.fileName);
					$("#vcreator").text(f.fileCreator);
					$("#vcdate").text(f.fileCreationDate);
					var fileSizeToInt = parseInt(f.fileSize);// 将文件体积（MB）数值转化为整型
					if (fileSizeToInt == 0) {
						// 文件体积小于1MB时
						$("#vsize").text("<1 MB");
					} else if (fileSizeToInt < 1000) {
						// 文件体积大于1MB但小于1000MB时
						$("#vsize").text(fileSizeToInt + " MB");
					} else if (fileSizeToInt < 1024000) {
						// 文件体积大于1000MB但小于1000GB时
						$("#vsize").text((fileSizeToInt / 1024).toFixed(2) + " GB");
					} else {
						// 文件体积大于1000GB
						$("#vsize").text((fileSizeToInt / 1048576).toFixed(2) + " TB");
					}
					if (f.needEncode == "N") {
						playVideo();
					} else {
						$("#playerMassage")
							.html(
								"<h2>播放器正在努力解码中...</h2><h3>已完成：<span id='transcodeProgress'>0</span>%</h3><p class='text-muted'>提示：该视频需解码后播放，请耐心等待！</p>");
						doTranscode();
					}
				} else {
					alert("错误：无法定位要预览的文件或该操作未被授权。");
					reMainPage();
				}
			},
			error: function() {
				alert("错误：请求失败，请刷新重试。");
				reMainPage();
			}
		});
});
// 获取URL上的视频id参数，它必须是第一个参数。
function getFileId() {
	var url = location.search;
	if (url.indexOf("?") != -1) {
		var str = url.substr(1);
		strs = str.split("=");
		return strs[1];
	}
	return "";
}
// 显示视频信息并播放视频
function playVideo() {
	$("#playerbox")
		.html(
			"<video id='kiftplayer' class='video-js col-md-12' controls preload='auto' height='500'>"
			+ "<source src='resourceController/getResource/"
			+ f.fileId + "' type='video/mp4'></video>");

	createComponent();//创建快进快退组件
	document.body.addEventListener('keydown',function(e){
		if (e.keyCode==39){//键盘-> 快进
			play_fast_next();
		}else if(e.keyCode==37){//键盘<- 快退
			play_fast_back();
		}else if(e.keyCode==32){//键盘 空格
			var text = document.getElementsByClassName("vjs-play-control")[0].innerText;
			if(text=='Play'){
				videoPlayer.play();
			}else if(text=='Pause'){
				videoPlayer.pause();
			}
		}
	});
	var player = videojs('kiftplayer', {
		preload: 'auto',
		playbackRates: [0.5, 1, 1.25, 1.5, 2],
		controlBar: {//控制按钮顺序
			children: ['backwardButton', 'playToggle', 'FastForwardButton', 'volumePanel'
				, 'currentTimeDisplay', 'timeDivider', 'durationDisplay', 'progressControl'
				, 'liveDisplay', 'seekToLive', 'remainingTimeDisplay'
				, 'customControlSpacer', 'playbackRateMenuButton', 'chaptersButton', 'descriptionsButton'
				, 'subsCapsButton', 'audioTrackButton', 'fullscreenToggle']
		}
	});

	videoPlayer = player;

	player.ready(function() {
		this.play();
	});
}

//================================增加快进快退按钮相关

function createComponent(){
	var baseComponent = videojs.getComponent('Component')
	var FastForwardButton = videojs.extend(baseComponent, {
		constructor: function(player, options) {
			baseComponent.apply(this, arguments)
			this.on('click', this.clickfastForward)
		},
		createEl: function() {
			var divObj = videojs.dom.createEl('button', {
				title: '快进十秒',
				style:'font-size:2em;width: 2em;',
				// className: 'vjs-fast-forward-button vjs-control vjs-button',
				className: 'vjs-icon-next-item vjs-fast-forward-button vjs-control vjs-button',
				innerHTML: '<span aria-hidden="true" class="vjs-icon-placeholder"></span><span class="vjs-control-text" aria-live="polite">快进</span>'
			})
			return divObj
		},
		clickfastForward: function() {
			play_fast_next();
		}
	})
	videojs.registerComponent('FastForwardButton', FastForwardButton);

	var backwardButton = videojs.extend(baseComponent, {
		constructor: function(player, options) {
			baseComponent.apply(this, arguments)
			this.on('click', this.clickBackward)
		},
		createEl: function() {
			var divObj = videojs.dom.createEl('button', {
				title: '快退十秒',
				style:'font-size:2em;width: 2em;',
				className: 'vjs-icon-previous-item vjs-fast-replay-button vjs-control vjs-button',
				innerHTML: '<span aria-hidden="true" class="vjs-icon-placeholder"></span><span class="vjs-control-text" aria-live="polite">快退</span>'
			})
			return divObj
		},
		clickBackward: function() {
			play_fast_back();
		}
	})
	videojs.registerComponent('backwardButton', backwardButton);
}

// 快进快退触发事件
/* 控制播放器快进10秒 */
var videoPlayer;
function play_fast_next() {
	videoPlayer.currentTime(videoPlayer.currentTime()+10);
}


/* 控制播放器后退10秒 */

function play_fast_back() {
	videoPlayer.currentTime(videoPlayer.currentTime()-10);
}
//================================

// 关闭当前窗口并释放播放器
function reMainPage() {
	if (tReq != null) {
		tReq.abort()
	}
	if (tTimer != null) {
		window.clearTimeout(tTimer);
	}
	window.opener = null;
	window.open('', '_self');
	window.close();
}

// 进行转码请求并监听进度状态（轮询）
function doTranscode() {
	tReq = $.ajax({
		url: 'resourceController/getVideoTranscodeStatus.ajax',
		type: 'POST',
		dataType: 'text',
		data: {
			fileId: f.fileId
		},
		success: function(result) {
			if (result == "FIN") {
				playVideo();
			} else if (result == "ERROR") {
				alert("错误：请求失败，请刷新重试。");
				reMainPage();
			} else {
				$("#transcodeProgress").text(result);
				tTimer = setTimeout('doTranscode()', 500);// 每隔1秒询问一次进度
			}
		},
		error: function() {
			alert("错误：请求失败，请刷新重试。");
			reMainPage();
		}
	});
}

function showCloseBtn() {
	var win = $(window).width();
	if (win < 450) {
		$("#closeBtn").addClass("hidden");
	} else {
		$("#closeBtn").removeClass("hidden");
	}
}

// 防止播放视频时会话超时的应答器，每分钟应答一次
function ping() {
	$.ajax({
		url: "homeController/ping.ajax",
		type: "POST",
		dataType: "text",
		data: {},
		success: function(result) {
			if (result != 'pong') {
				window.clearInterval(pingInt);
			}
		},
		error: function() {
			window.clearInterval(pingInt);
		}
	});
}