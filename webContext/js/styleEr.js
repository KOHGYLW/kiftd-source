// 显示加载动画
function showLoader() {
    const loader = document.createElement('div');
    loader.className = 'loader-wrapper';
    loader.innerHTML = '<div class="loader"></div>';
    document.body.appendChild(loader);
    return loader;
}

// 隐藏加载动画
function hideLoader(loader) {
    if (loader) {
        loader.style.opacity = '0';
        setTimeout(() => {
            if (loader.parentNode) {
                loader.parentNode.removeChild(loader);
            }
        }, 500);
    }
}

// 加载 CSS 文件
function loadCSS(url, callback) {
    const link = document.createElement('link');
    link.rel = 'stylesheet';
    link.href = url;
    
    if (callback) {
        link.onload = () => callback(true);
        link.onerror = () => {
            console.error('Failed to load CSS:', url);
            callback(false);
        };
    }
    
    document.head.appendChild(link);
    return link;
}

// 获取当前样式 URL
function getUrl(defaultUrl = '/css/bootstrap.min.css') {
    return localStorage.getItem('styleUrl') || defaultUrl;
}

// 设置样式 URL
function setUrl(url) {
    localStorage.setItem('styleUrl', url);
}

// 根据当前样式设置logo图片
function setLogoImage(defaultUrl = '/css/bootstrap.min.css') {
    const logoImg = document.getElementById('lg');
    if (!logoImg) return;
    
    const currentUrl = getUrl(defaultUrl);
    const isDefaultStyle = currentUrl === defaultUrl;
    
    // 根据当前使用的样式设置对应的logo图片
    logoImg.src = isDefaultStyle ? '/l.png' : '/d.png';
}

// 初始化页面样式
function initStyle(defaultUrl = '/css/bootstrap.min.css', alternateUrl = '/css/bootstrap.min.dark.css') {
    // 预加载两种样式
    const preload1 = document.createElement('link');
    preload1.rel = 'preload';
    preload1.as = 'style';
    preload1.href = defaultUrl;
    document.head.appendChild(preload1);
    
    const preload2 = document.createElement('link');
    preload2.rel = 'preload';
    preload2.as = 'style';
    preload2.href = alternateUrl;
    document.head.appendChild(preload2);
    
    const url = getUrl(defaultUrl);
    const loader = showLoader();
    
    loadCSS(url, (success) => {
        if (success) {
            // 设置logo图片
            setLogoImage(defaultUrl);
            // 显示页面内容
            document.body.style.visibility = 'visible';
        }
        hideLoader(loader);
    });
}

// 切换样式
function changeStyle(defaultUrl = '/css/bootstrap.min.css', alternateUrl = '/css/bootstrap.min.orange.css') {
    const currentUrl = getUrl(defaultUrl);
    const newUrl = currentUrl === defaultUrl ? alternateUrl : defaultUrl;
    
    setUrl(newUrl);
    window.location.reload();
}
