const API_BASE = new URL("./api/kb", document.baseURI).pathname.replace(/\/$/, "");
const DOCUMENTS_CONTEXT_KEY = "kbAgent.documentsContext";
const MESSAGE_AUTO_HIDE_MS = 3200;
const STATUS_META = {
    READY: { className: "status-ready", label: "已就绪" },
    PROCESSING: { className: "status-processing", label: "索引中" },
    FAILED: { className: "status-failed", label: "失败" }
};

const state = {
    documentId: resolveDocumentId(),
    documentContent: {
        documentId: null,
        status: "idle",
        content: "",
        error: ""
    },
    messageTimer: null,
    documentsContext: readDocumentsContext()
};

const refs = {};

document.addEventListener("DOMContentLoaded", () => {
    cacheElements();
    bindEvents();
    updateBackLink();

    if (!state.documentId) {
        refs.documentDetailView.innerHTML = "<p>无法识别当前文档 ID，请从文档中心重新打开详情页。</p>";
        showMessage("error", "详情页地址无效");
        return;
    }

    loadDocumentDetail().catch(handlePageError);
});

function cacheElements() {
    refs.message = document.getElementById("detail-message");
    refs.refreshButton = document.getElementById("detail-refresh");
    refs.documentDetailView = document.getElementById("document-detail-view");
    refs.backLink = document.getElementById("detail-back-link");
}

function bindEvents() {
    refs.refreshButton.addEventListener("click", () => {
        loadDocumentDetail(true).catch(handlePageError);
    });
}

function resolveDocumentId() {
    const match = window.location.pathname.match(/\/details-(\d+)\/?$/);
    return match ? match[1] : "";
}

async function request(path, options = {}) {
    const response = await fetch(`${API_BASE}${path}`, options);
    let payload;
    try {
        payload = await response.json();
    } catch (error) {
        throw new Error("服务返回了非 JSON 响应");
    }

    if (!response.ok) {
        throw new Error(payload?.message || `HTTP ${response.status}`);
    }
    if (!payload || payload.code !== 200) {
        throw new Error(payload?.message || "请求失败");
    }
    return payload.data;
}

async function loadDocumentDetail(showToast = false) {
    setButtonBusy(refs.refreshButton, true, "刷新中");
    refs.documentDetailView.innerHTML = "<p>正在读取文档元数据...</p>";

    try {
        const documentDetail = await request(`/documents/${state.documentId}`);
        state.documentContent = {
            documentId: state.documentId,
            status: "loading",
            content: "",
            error: ""
        };
        renderDocumentDetail(documentDetail);
        persistDocumentsContext({ selectedDocumentId: normalizeOptionalId(state.documentId) });
        document.title = `${documentDetail.fileName || `文档 ${state.documentId}`} · 文档详情`;
        loadDocumentContent(state.documentId).catch(console.error);

        if (showToast) {
            showMessage("info", `已刷新文档 #${state.documentId} 的详情。`);
        }
    } finally {
        setButtonBusy(refs.refreshButton, false, "");
    }
}

function renderDocumentDetail(documentDetail) {
    const errorBlockClass = documentDetail.errorMessage ? "detail-copy detail-page-copy has-error" : "detail-copy detail-page-copy";

    refs.documentDetailView.innerHTML = `
        <div class="detail-page-card-head">
            <div class="detail-title">
                <h3 data-testid="detail-document-title">${escapeHtml(documentDetail.fileName || "文档详情")}</h3>
                ${renderStatus(documentDetail.status)}
            </div>
            <p class="detail-page-doc-id" data-testid="detail-document-id">文档 ID #${escapeHtml(documentDetail.documentId)}</p>
        </div>
        <div class="meta-grid detail-page-meta-grid">
            <div class="meta-item">
                <span>文档 ID</span>
                <strong>${escapeHtml(documentDetail.documentId)}</strong>
            </div>
            <div class="meta-item">
                <span>知识库 ID</span>
                <strong>${escapeHtml(documentDetail.kbId)}</strong>
            </div>
            <div class="meta-item">
                <span>文件类型</span>
                <strong>${escapeHtml(formatSourceType(documentDetail.sourceType))}</strong>
            </div>
            <div class="meta-item">
                <span>标签</span>
                <div class="tag-list">${documentDetail.tags && documentDetail.tags.length ? renderTags(documentDetail.tags) : "—"}</div>
            </div>
            <div class="meta-item">
                <span>创建时间</span>
                <strong>${escapeHtml(formatDateTime(documentDetail.createdAt))}</strong>
            </div>
            <div class="meta-item">
                <span>索引时间</span>
                <strong>${escapeHtml(formatDateTime(documentDetail.indexedAt))}</strong>
            </div>
        </div>
        <div class="${errorBlockClass}" data-testid="detail-error-block">
            ${documentDetail.errorMessage
                ? `<strong>错误信息：</strong> ${escapeHtml(documentDetail.errorMessage)}`
                : "当前文档没有错误信息。"}
        </div>
        <section class="detail-section">
            <div class="detail-section-head">
                <h4>文档正文</h4>
                <p class="detail-section-note">按纯文本展示，避免执行文档中的 HTML 或脚本。</p>
            </div>
            <div class="document-content-panel detail-page-content-panel" data-role="document-content" data-testid="detail-document-content">${renderDocumentContent()}</div>
        </section>
    `;
}

function renderDocumentContent() {
    const currentContent = state.documentContent;
    if (!currentContent || currentContent.documentId !== state.documentId) {
        return '<p class="document-content-placeholder">正文尚未开始加载。</p>';
    }
    if (currentContent.status === "loading") {
        return '<p class="document-content-loading">正文加载中...</p>';
    }
    if (currentContent.status === "error") {
        return `<p class="document-content-error">正文加载失败：${escapeHtml(currentContent.error || "未知错误")}</p>`;
    }
    if (!currentContent.content) {
        return '<p class="document-content-empty">文档正文为空。</p>';
    }
    return `<div class="document-content-body">${escapeHtml(currentContent.content)}</div>`;
}

function updateDocumentContentPanel() {
    const contentPanel = refs.documentDetailView.querySelector('[data-role="document-content"]');
    if (contentPanel) {
        contentPanel.innerHTML = renderDocumentContent();
    }
}

async function loadDocumentContent(documentId) {
    try {
        const documentContent = await request(`/documents/${documentId}/content`);
        if (state.documentId !== documentId) {
            return;
        }
        state.documentContent = {
            documentId,
            status: "loaded",
            content: documentContent.content || "",
            error: ""
        };
    } catch (error) {
        if (state.documentId !== documentId) {
            return;
        }
        state.documentContent = {
            documentId,
            status: "error",
            content: "",
            error: error.message || "请求失败"
        };
    }
    updateDocumentContentPanel();
}

function setButtonBusy(button, isBusy, busyText) {
    if (!button) {
        return;
    }

    if (isBusy) {
        if (!button.dataset.originalText) {
            button.dataset.originalText = button.textContent;
        }
        button.disabled = true;
        button.classList.add("is-loading");
        button.textContent = busyText;
        return;
    }

    button.disabled = false;
    button.classList.remove("is-loading");
    if (button.dataset.originalText) {
        button.textContent = button.dataset.originalText;
    }
}

function hideMessage() {
    window.clearTimeout(state.messageTimer);
    state.messageTimer = null;
    refs.message.classList.add("hidden");
}

function showMessage(type, message) {
    window.clearTimeout(state.messageTimer);
    refs.message.className = `message ${type}`;
    refs.message.textContent = message;
    refs.message.classList.remove("hidden");

    if (type !== "error") {
        state.messageTimer = window.setTimeout(() => {
            hideMessage();
        }, MESSAGE_AUTO_HIDE_MS);
    }
}

function handlePageError(error) {
    console.error(error);
    refs.documentDetailView.innerHTML = `<p>${escapeHtml(error.message || "页面加载失败")}</p>`;
    showMessage("error", error.message || "页面加载失败");
}

function escapeHtml(value) {
    return String(value ?? "")
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

function formatDateTime(value) {
    if (!value) {
        return "—";
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return new Intl.DateTimeFormat("zh-CN", {
        year: "numeric",
        month: "2-digit",
        day: "2-digit",
        hour: "2-digit",
        minute: "2-digit",
        second: "2-digit"
    }).format(date);
}

function renderStatus(status) {
    const meta = STATUS_META[status] || { className: "status-processing", label: "处理中" };
    return `
        <span class="status-badge ${meta.className}">
            <span>${escapeHtml(meta.label)}</span>
            <small>${escapeHtml(status || "UNKNOWN")}</small>
        </span>
    `;
}

function renderTags(tags) {
    if (!tags || tags.length === 0) {
        return "—";
    }
    return tags.map((tag) => `<span class="tag-pill">${escapeHtml(tag)}</span>`).join("");
}

function formatSourceType(value) {
    if (!value) {
        return "—";
    }
    return String(value).toUpperCase();
}

function readDocumentsContext() {
    try {
        const rawValue = window.sessionStorage.getItem(DOCUMENTS_CONTEXT_KEY);
        if (!rawValue) {
            return null;
        }
        const parsed = JSON.parse(rawValue);
        return parsed && typeof parsed === "object" ? parsed : null;
    } catch (error) {
        console.warn("无法读取文档中心上下文", error);
        return null;
    }
}

function updateBackLink() {
    const context = state.documentsContext;
    if (!refs.backLink) {
        return;
    }
    if (!context) {
        refs.backLink.href = "./#documents";
        return;
    }
    refs.backLink.href = `./#${encodeURIComponent(context.activeSection || "documents")}`;
}

function persistDocumentsContext(overrides = {}) {
    try {
        const nextContext = {
            activeSection: overrides.activeSection || state.documentsContext?.activeSection || "documents",
            filter: {
                kbId: typeof overrides.kbId === "string"
                    ? overrides.kbId.trim()
                    : (state.documentsContext?.filter?.kbId || "default-kb"),
                status: typeof overrides.status === "string"
                    ? overrides.status
                    : (state.documentsContext?.filter?.status || ""),
                fileName: typeof overrides.fileName === "string"
                    ? overrides.fileName
                    : (state.documentsContext?.filter?.fileName || "")
            },
            pageNum: normalizePositiveInt(overrides.pageNum, state.documentsContext?.pageNum || 1),
            pageSize: normalizePositiveInt(overrides.pageSize, state.documentsContext?.pageSize || 10),
            selectedDocumentId: normalizeOptionalId(
                overrides.selectedDocumentId !== undefined
                    ? overrides.selectedDocumentId
                    : state.documentsContext?.selectedDocumentId
            )
        };
        state.documentsContext = nextContext;
        window.sessionStorage.setItem(DOCUMENTS_CONTEXT_KEY, JSON.stringify(nextContext));
        updateBackLink();
    } catch (error) {
        console.warn("无法保存文档中心上下文", error);
    }
}

function normalizePositiveInt(value, fallback) {
    const normalizedValue = Number(value);
    if (Number.isInteger(normalizedValue) && normalizedValue > 0) {
        return normalizedValue;
    }
    return fallback;
}

function normalizeOptionalId(value) {
    const normalizedValue = String(value ?? "").trim();
    return normalizedValue ? normalizedValue : null;
}
