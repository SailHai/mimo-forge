import * as vscode from 'vscode';
import OpenAI from 'openai';

/**
 * MiMo Forge VSCode Extension
 *
 * 集成 Xiaomi MiMo V2.5 的 AI 编程助手，提供：
 * - 智能代码补全（Inline Completion）
 * - AI 对话（Chat Panel）
 * - 代码解释 / 重构 / 审查
 * - Agent Pipeline 执行
 * - RAG 知识库检索
 *
 * @author Senior AI Engineer
 */

let mimoClient: OpenAI;

export function activate(context: vscode.ExtensionContext) {
    const config = vscode.workspace.getConfiguration('mimo');
    const apiKey = config.get<string>('apiKey') || '';
    const serverUrl = config.get<string>('serverUrl') || 'https://api.mimo.xiaomi.com/v1';
    const model = config.get<string>('model') || 'MiMo-Max';

    // 初始化 MiMo API 客户端（OpenAI 兼容格式）
    mimoClient = new OpenAI({
        apiKey,
        baseURL: serverUrl,
    });

    // ═══════════════════════════════════════════
    //  注册命令
    // ═══════════════════════════════════════════

    // AI Chat 面板
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.chat', () => {
            MiMoChatPanel.createOrShow(context.extensionUri, mimoClient, model);
        })
    );

    // 解释代码
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.explain', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) return;
            const code = editor.document.getText(editor.selection);
            if (!code) { vscode.window.showWarningMessage('请先选中代码'); return; }

            const lang = editor.document.languageId;
            await streamToOutputChannel('MiMo: Explain', mimoClient, model,
                `你是代码解释专家。请用中文详细解释以下 ${lang} 代码的功能、设计模式和关键逻辑：\n\n\`\`\`${lang}\n${code}\n\`\`\``);
        })
    );

    // 重构代码
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.refactor', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) return;
            const code = editor.document.getText(editor.selection);
            if (!code) { vscode.window.showWarningMessage('请先选中代码'); return; }

            const lang = editor.document.languageId;
            const result = await callMiMo(mimoClient, model,
                `你是代码重构专家。请重构以下 ${lang} 代码，提升可读性、可维护性和性能，保持功能不变。只输出重构后的代码：\n\n\`\`\`${lang}\n${code}\n\`\`\``);

            if (result) {
                await editor.edit(editBuilder => {
                    editBuilder.replace(editor.selection, result);
                });
            }
        })
    );

    // 代码审查
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.review', async () => {
            const editor = vscode.window.activeTextEditor;
            if (!editor) return;
            const code = editor.document.getText(editor.selection);
            if (!code) { vscode.window.showWarningMessage('请先选中代码'); return; }

            const lang = editor.document.languageId;
            await streamToOutputChannel('MiMo: Code Review', mimoClient, model,
                `你是代码审查专家。请审查以下 ${lang} 代码，按以下维度评估（每项 1-10 分）：\n` +
                `1. 正确性 2. 可读性 3. 性能 4. 安全性 5. 可测试性\n\n` +
                `代码：\n\`\`\`${lang}\n${code}\n\`\`\`\n\n` +
                `请输出：评分表 + 发现的问题 + 具体修改建议。`);
        })
    );

    // 代码生成
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.generate', async () => {
            const prompt = await vscode.window.showInputBox({
                prompt: '描述你想要生成的代码',
                placeHolder: '例如：一个 Spring Boot REST API，实现用户注册和登录'
            });
            if (!prompt) return;

            const editor = vscode.window.activeTextEditor;
            const lang = editor?.document.languageId || 'typescript';
            const result = await callMiMo(mimoClient, model,
                `你是高级工程师。请生成生产级 ${lang} 代码，要求：可编译运行、包含错误处理和日志。需求：${prompt}`);

            if (result && editor) {
                await editor.edit(editBuilder => {
                    editBuilder.insert(editor.selection.active, result);
                });
            }
        })
    );

    // Agent Pipeline
    context.subscriptions.push(
        vscode.commands.registerCommand('mimo.agent.pipeline', async () => {
            const prompt = await vscode.window.showInputBox({
                prompt: '输入需求描述，启动 Agent Pipeline',
                placeHolder: '例如：开发一个任务管理系统，支持 CRUD、权限和通知'
            });
            if (!prompt) return;

            await streamToOutputChannel('MiMo: Agent Pipeline', mimoClient, model,
                `启动完整 Agent Pipeline，需求如下：\n${prompt}\n\n` +
                `请按以下阶段执行：\n1. 需求分析 2. 架构设计 3. 代码生成 4. 代码审查 5. 测试用例\n` +
                `每个阶段输出详细结果。`);
        })
    );

    // ═══════════════════════════════════════════
    //  Inline Completion Provider (MiMo-Lite)
    // ═══════════════════════════════════════════

    if (config.get<boolean>('enableInlineCompletion')) {
        const provider: vscode.InlineCompletionItemProvider = {
            provideInlineCompletionItems: async (document, position, context, token) => {
                const linePrefix = document.lineAt(position).text.substring(0, position.character);
                if (linePrefix.trim().length < 3) return;

                // 获取光标前后的上下文
                const startLine = Math.max(0, position.line - 30);
                const endLine = Math.min(document.lineCount - 1, position.line + 10);
                const contextCode = document.getText(new vscode.Range(startLine, 0, endLine, 0));

                try {
                    const response = await mimoClient.chat.completions.create({
                        model: 'MiMo-Lite',
                        messages: [
                            { role: 'system', content: '你是代码补全专家。根据上下文补全代码，只输出补全部分，不要解释。' },
                            { role: 'user', content: `语言: ${document.languageId}\n上下文:\n${contextCode}\n\n补全光标位置后的代码：` }
                        ],
                        max_tokens: 256,
                        temperature: 0.1,
                    });

                    const completion = response.choices[0]?.message?.content;
                    if (completion && !token.isCancellationRequested) {
                        return [new vscode.InlineCompletionItem(completion)];
                    }
                } catch (e) {
                    // 静默失败，不打断用户输入
                }
            }
        };

        context.subscriptions.push(
            vscode.languages.registerInlineCompletionItemProvider(
                { pattern: '**' }, provider
            )
        );
    }

    // 状态栏
    const statusBar = vscode.window.createStatusBarItem(vscode.StatusBarAlignment.Right, 100);
    statusBar.text = "$(spark) MiMo Max";
    statusBar.tooltip = "MiMo Forge AI Assistant — Click to chat";
    statusBar.command = 'mimo.chat';
    statusBar.show();
    context.subscriptions.push(statusBar);

    console.log('MiMo Forge activated');
}

// ═══════════════════════════════════════════
//  工具函数
// ═══════════════════════════════════════════

async function callMiMo(client: OpenAI, model: string, prompt: string): Promise<string | null> {
    try {
        const response = await client.chat.completions.create({
            model,
            messages: [{ role: 'user', content: prompt }],
            max_tokens: vscode.workspace.getConfiguration('mimo').get<number>('maxTokens') || 8192,
            temperature: vscode.workspace.getConfiguration('mimo').get<number>('temperature') || 0.3,
        });
        return response.choices[0]?.message?.content || null;
    } catch (e: any) {
        vscode.window.showErrorMessage(`MiMo API Error: ${e.message}`);
        return null;
    }
}

async function streamToOutputChannel(name: string, client: OpenAI, model: string, prompt: string) {
    const channel = vscode.window.createOutputChannel(name);
    channel.show(true);
    channel.appendLine('═══ MiMo 生成中 ═══\n');

    try {
        const stream = await client.chat.completions.create({
            model,
            messages: [{ role: 'user', content: prompt }],
            max_tokens: 8192,
            temperature: 0.3,
            stream: true,
        });

        for await (const chunk of stream) {
            const content = chunk.choices[0]?.delta?.content;
            if (content) { channel.append(content); }
        }
        channel.appendLine('\n\n═══ 完成 ═══');
    } catch (e: any) {
        channel.appendLine(`\n错误: ${e.message}`);
    }
}

// ═══════════════════════════════════════════
//  Chat Panel Webview
// ═══════════════════════════════════════════

class MiMoChatPanel {
    public static currentPanel: MiMoChatPanel | undefined;
    private readonly _panel: vscode.WebviewPanel;

    static createOrShow(extensionUri: vscode.Uri, client: OpenAI, model: string) {
        if (MiMoChatPanel.currentPanel) {
            MiMoChatPanel.currentPanel._panel.reveal();
            return;
        }
        const panel = vscode.window.createWebviewPanel('mimoChat', 'MiMo AI Chat',
            vscode.ViewColumn.Two, { enableScripts: true });
        MiMoChatPanel.currentPanel = new MiMoChatPanel(panel, client, model);
    }

    constructor(panel: vscode.WebviewPanel, client: OpenAI, model: string) {
        this._panel = panel;
        this._panel.webview.html = this._getHtml();
        this._panel.webview.onDidReceiveMessage(async (msg) => {
            if (msg.type === 'chat') {
                const response = await callMiMo(client, model, msg.text);
                this._panel.webview.postMessage({ type: 'response', text: response });
            }
        });
        this._panel.onDidDispose(() => { MiMoChatPanel.currentPanel = undefined; });
    }

    private _getHtml(): string {
        return `<!DOCTYPE html><html><body style="font-family:system-ui;padding:16px;">
<h3>MiMo AI Chat</h3><div id="log" style="height:400px;overflow-y:auto;border:1px solid #333;padding:8px;margin:8px 0;"></div>
<input id="input" style="width:100%;padding:8px;" placeholder="输入问题..." onkeydown="if(event.key==='Enter')send()">
<script>const log=document.getElementById('log');const input=document.getElementById('input');
const vs=acquireVsCodeApi();
function send(){const t=input.value;if(!t)return;log.innerHTML+='<p><b>You:</b> '+t+'</p>';vs.postMessage({type:'chat',text:t});input.value='';}
window.addEventListener('message',e=>{if(e.data.type==='response')log.innerHTML+='<p><b>MiMo:</b> '+e.data.text+'</p>';log.scrollTop=log.scrollHeight;});
</script></body></html>`;
    }
}

export function deactivate() {}
