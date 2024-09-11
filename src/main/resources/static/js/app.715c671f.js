(function () {
    "use strict";
    var t = {
        6547: function (t, e, a) {
            var i = a(2856), l = function () {
                    var t = this, e = t._self._c;
                    return e("div", {attrs: {id: "app"}}, [e("router-view")], 1)
                }, s = [], n = {name: "App"}, r = n, o = a(1656), c = (0, o.A)(r, l, s, !1, null, null, null),
                d = c.exports, u = a(184), h = function () {
                    var t = this, e = t._self._c;
                    return e("div", {attrs: {id: "app"}}, [e("div", {staticClass: "login-container"}, [e("h1", {staticStyle: {"margin-left": "10px"}}, [t._v("Panel"), e("span", {
                        staticClass: "toggle-icon",
                        on: {
                            click: function (e) {
                                return t.switchToIndex()
                            }
                        }
                    }, [e("img", {
                        attrs: {
                            src: a(8902),
                            title: "切换至Pandora",
                            alt: "hi"
                        }
                    })])]), e("form", {
                        on: {
                            submit: function (e) {
                                return e.preventDefault(), t.userlogin()
                            }
                        }
                    }, [e("input", {
                        attrs: {
                            type: "text",
                            id: "username",
                            placeholder: "用户名",
                            required: ""
                        }
                    }), e("input", {
                        attrs: {
                            type: "password",
                            id: "password",
                            placeholder: "密码",
                            required: ""
                        }
                    }), t._m(0)]), t._m(1), e("div", {staticClass: "alternative-login"}, [e("div", {staticClass: "oauth-buttons"}, [e("img", {
                        attrs: {
                            src: a(4832),
                            alt: "LINUX DO"
                        }, on: {
                            click: function (e) {
                                return t.initiateOAuth()
                            }
                        }
                    })]), e("button", {
                        attrs: {type: "button"}, on: {
                            click: function (e) {
                                return t.reset()
                            }
                        }
                    }, [t._v("重置密码")])]), e("div", {
                        staticClass: "footer", on: {
                            click: function (e) {
                                return t.index()
                            }
                        }
                    }, [e("a", {
                        attrs: {
                            href: "https://github.com/Kylsky/pandora-helper-with-linux-do-oauth",
                            target: "_blank"
                        }
                    }, [t._v("Powered by Yeelo")])])])])
                }, p = [function () {
                    var t = this, e = t._self._c;
                    return e("button", {attrs: {type: "submit"}}, [e("span", {staticClass: "btn-text"}, [t._v("登录")]), e("span", {staticClass: "spinner"})])
                }, function () {
                    var t = this, e = t._self._c;
                    return e("div", {staticClass: "divider"}, [e("span", [t._v("OR")])])
                }];
            const m = {apiBaseUrl: window.API_URL};
            var f = m, g = a(7423);
            const b = g.A.create({
                baseURL: window.API_URL,
                headers: {"Content-Type": "application/json"},
                timeout: 1e4
            });
            var v = b, y = {
                name: "IndexPage", props: {}, data() {
                    return {}
                }, methods: {
                    reset() {
                        this.$router.replace({name: "reset"})
                    }, async userlogin() {
                        var t = document.querySelector(".spinner"), e = document.querySelector(".btn-text");
                        e.style.visibility = "hidden", t.style.display = "block";
                        var a = document.getElementById("username").value,
                            i = document.getElementById("password").value;
                        try {
                            const l = await v.post(`${f.apiBaseUrl}/user/login`, {username: a, password: i});
                            this.loading = !1;
                            const s = l.data.status;
                            if (!s) return alert(l.data.message), t.style.display = "none", e.style.visibility = "visible", !1;
                            const n = l.data, r = n.data.jwt, o = n.data.avatarUrl;
                            localStorage.setItem("token", r), o && localStorage.setItem("img", o), t.style.display = "none", e.textContent = "登录成功,跳转中...", e.style.visibility = "visible", this.$router.replace({name: "navi"})
                        } catch (s) {
                            t.style.display = "none", e.style.visibility = "visible";
                            var l = l;
                            alert(l)
                        }
                    }, async initiateOAuth() {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/oauth2/initiate?type=panel`);
                            var t = e;
                            console.log(t), t.data ? window.location = t.data : console.error("No redirect URL provided")
                        } catch (e) {
                            console.error("Request failed with status:", e)
                        }
                    }, switchToIndex() {
                        this.$router.replace({name: "pandora"})
                    }
                }
            }, w = y, C = (0, o.A)(w, h, p, !1, null, "2e2813ef", null), _ = C.exports, x = function () {
                var t = this, e = t._self._c;
                return e("div", {attrs: {id: "reset"}}, [e("div", {staticClass: "reset-container"}, [e("h1", [t._v("重置密码")]), e("form", {
                    on: {
                        submit: function (e) {
                            return e.preventDefault(), t.resetPassword()
                        }
                    }
                }, [e("input", {
                    attrs: {
                        type: "text",
                        id: "username",
                        placeholder: "用户名",
                        required: ""
                    }
                }), e("input", {
                    attrs: {
                        type: "password",
                        id: "oldPassword",
                        placeholder: "旧密码",
                        required: ""
                    }
                }), e("input", {
                    attrs: {
                        type: "password",
                        id: "newPassword",
                        placeholder: "新密码",
                        required: ""
                    }
                }), e("input", {
                    attrs: {
                        type: "password",
                        id: "confirmPassword",
                        placeholder: "再次确认新密码",
                        required: ""
                    }
                }), e("button", {attrs: {type: "submit"}}, [t._v("重置")]), e("button", {
                    staticClass: "home-button",
                    attrs: {type: "button"},
                    on: {
                        click: function (e) {
                            return t.goHome()
                        }
                    }
                }, [t._v("返回主页")])]), e("div", {staticClass: "footer"}, [t._v(" Powered by Yeelo ")])])])
            }, I = [], k = {
                name: "ResetPage", methods: {
                    async resetPassword() {
                        var t = document.getElementById("username").value,
                            e = document.getElementById("oldPassword").value,
                            a = document.getElementById("newPassword").value,
                            i = document.getElementById("confirmPassword").value;
                        if (e === a) return alert("新密码不能和旧密码相同，请重新输入。"), !1;
                        if (a.length < 8) return alert("密码长度必须超过大于等于8位，请重新输入。"), !1;
                        if (a !== i) return alert("两次输入的密码不一致，请重新输入。"), !1;
                        try {
                            const l = await v.post(`${f.apiBaseUrl}/pandora/reset`, {
                                username: t,
                                oldPassword: e,
                                newPassword: a,
                                confirmPassword: i
                            }), s = l.data.status;
                            s ? (alert("密码重置成功！"), this.$router.replace({name: "home"})) : alert(l.data.message)
                        } catch (l) {
                            alert(l)
                        }
                    }, async goHome() {
                        this.$router.replace({name: "home"})
                    }
                }
            }, V = k, F = (0, o.A)(V, x, I, !1, null, "ca797f4e", null), S = F.exports, A = function () {
                var t = this, e = t._self._c;
                return e("div", {attrs: {id: "pandora"}}, [e("div", {staticClass: "login-container"}, [e("h1", {staticStyle: {"margin-left": "10px"}}, [t._v("Pandora"), e("span", {
                    staticClass: "toggle-icon",
                    on: {
                        click: function (e) {
                            return t.switchToFuclaude()
                        }
                    }
                }, [e("img", {
                    attrs: {
                        src: a(8902),
                        title: "切换至Fuclaude",
                        alt: "hi"
                    }
                })])]), e("form", {
                    on: {
                        submit: function (e) {
                            return e.preventDefault(), t.userlogin()
                        }
                    }
                }, [e("input", {
                    attrs: {
                        type: "text",
                        id: "username",
                        placeholder: "用户名",
                        required: ""
                    }
                }), e("input", {
                    attrs: {
                        type: "password",
                        id: "password",
                        placeholder: "密码",
                        required: ""
                    }
                }), t._m(0)]), t._m(1), e("div", {staticClass: "alternative-login"}, [e("div", {staticClass: "oauth-buttons"}, [e("img", {
                    attrs: {
                        src: a(4832),
                        alt: "LINUX DO"
                    }, on: {
                        click: function (e) {
                            return t.initiateOAuth()
                        }
                    }
                })]), e("button", {
                    attrs: {type: "button"}, on: {
                        click: function (e) {
                            return t.reset()
                        }
                    }
                }, [t._v("重置密码")])]), e("div", {staticClass: "footer"}, [t._v(" Powered by Pandora ")])])])
            }, D = [function () {
                var t = this, e = t._self._c;
                return e("button", {attrs: {type: "submit"}}, [e("span", {staticClass: "btn-text"}, [t._v("登录")]), e("span", {staticClass: "spinner"})])
            }, function () {
                var t = this, e = t._self._c;
                return e("div", {staticClass: "divider"}, [e("span", [t._v("OR")])])
            }], P = {
                name: "PandoraPage", methods: {
                    reset() {
                        this.$router.replace({name: "reset"})
                    }, async userlogin() {
                        var t = document.querySelector(".spinner"), e = document.querySelector(".btn-text");
                        e.style.visibility = "hidden", t.style.display = "block";
                        var a = document.getElementById("username").value,
                            i = document.getElementById("password").value;
                        try {
                            const s = await v.post(`${f.apiBaseUrl}/pandora/login`, {username: a, password: i}),
                                n = s.data.status;
                            if (n) t.style.display = "none", e.textContent = "登录成功,跳转中...", e.style.visibility = "visible", window.open(s.data.data), e.textContent = "登录"; else {
                                t.style.display = "none", e.style.visibility = "visible";
                                var l = s.data.message;
                                alert(l)
                            }
                        } catch (s) {
                            t.style.display = "none", e.style.visibility = "visible", alert(s)
                        }
                    }, async initiateOAuth() {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/oauth2/initiate?type=ChatGPT`);
                            var t = e;
                            console.log(t), t.data ? window.location = t.data : console.error("No redirect URL provided")
                        } catch (e) {
                            alert(e)
                        }
                    }, switchToFuclaude() {
                        this.$router.replace({name: "claude"})
                    }
                }
            }, B = P, T = (0, o.A)(B, A, D, !1, null, "17757e0e", null), $ = T.exports, q = function () {
                var t = this;
                t._self._c;
                return t._m(0)
            }, U = [function () {
                var t = this, e = t._self._c;
                return e("div", {attrs: {id: "loading"}}, [e("div", [e("div", {staticClass: "loader"}), e("p", {staticClass: "message"}, [t._v("正在授权，请稍候...")])])])
            }], z = (a(4603), a(7566), a(8721), {
                name: "LoadingPage", methods: {
                    async fetchToken(t, e) {
                        console.log(t, e);
                        const a = await v.get(`${f.apiBaseUrl}/oauth2/callback?code=${t}&state=${e}`, {withCredentials: !0});
                        if (a.data.status) {
                            let t = JSON.parse(a.data.data);
                            console.log(t.avatar_url), localStorage.setItem("img", t.avatar_url), "ChatGPT" === t.shareType ? await this.startGPTChat(t.username, t.jmc) : "Claude" === t.shareType ? await this.startClaudeChat(t.username, t.jmc) : "panel" === t.shareType && await this.startPanel(t.username, t.jmc)
                        }
                    }, async startGPTChat(t, e) {
                        const a = await v.get(`${f.apiBaseUrl}/pandora/checkUser?username=${t}&jmc=${e}`, {withCredentials: !0});
                        a.data.status && a.data.data.isShared ? window.open(a.data.data) : (alert("您的账号未激活，请联系管理员"), localStorage.setItem("token", a.data.data), this.$router.replace({name: "pandora"}))
                    }, async startClaudeChat(t, e) {
                        const a = await v.get(`${f.apiBaseUrl}/fuclaude/checkUser?username=${t}&jmc=${e}`, {withCredentials: !0});
                        a.data.status && a.data.data.isShared ? window.open(a.data.data) : (alert("您的账号未激活，请联系管理员"), localStorage.setItem("token", a.data.data), this.$router.replace({name: "claude"}))
                    }, async startPanel(t, e) {
                        console.log("hi");
                        const a = await v.get(`${f.apiBaseUrl}/share/checkUser?username=${t}&jmc=${e}`, {withCredentials: !0});
                        a.data.status ? (localStorage.setItem("token", a.data.data), this.$router.replace({name: "navi"})) : (alert(a.data.message), this.$router.replace({name: ""}))
                    }
                }, mounted() {
                    const t = new URL(window.location.href), e = new URLSearchParams(t.search), a = e.get("code"),
                        i = e.get("state");
                    a && i && this.fetchToken(a, i)
                }
            }), O = z, E = (0, o.A)(O, q, U, !1, null, "1d5c951b", null), N = E.exports, M = function () {
                var t = this, e = t._self._c;
                return e("div", {attrs: {id: "claude"}}, [e("div", {staticClass: "login-container"}, [e("h1", {staticStyle: {"margin-left": "10px"}}, [t._v("Fuclaude"), e("span", {
                    staticClass: "toggle-icon",
                    on: {
                        click: function (e) {
                            return t.switchToPandora()
                        }
                    }
                }, [e("img", {
                    attrs: {
                        src: a(8902),
                        title: "切换至Panel",
                        alt: "hi"
                    }
                })])]), e("form", {
                    on: {
                        submit: function (e) {
                            return e.preventDefault(), t.userlogin()
                        }
                    }
                }, [e("input", {
                    attrs: {
                        type: "text",
                        id: "username",
                        placeholder: "用户名",
                        required: ""
                    }
                }), e("input", {
                    attrs: {
                        type: "password",
                        id: "password",
                        placeholder: "密码",
                        required: ""
                    }
                }), t._m(0)]), t._m(1), e("div", {staticClass: "alternative-login"}, [e("div", {staticClass: "oauth-buttons"}, [e("img", {
                    attrs: {
                        src: a(4832),
                        alt: "LINUX DO"
                    }, on: {
                        click: function (e) {
                            return t.initiateOAuth()
                        }
                    }
                })]), e("button", {
                    attrs: {type: "button"}, on: {
                        click: function (e) {
                            return t.reset()
                        }
                    }
                }, [t._v("重置密码")])]), e("div", {staticClass: "footer"}, [t._v(" Powered by Fuclaude ")])])])
            }, j = [function () {
                var t = this, e = t._self._c;
                return e("button", {attrs: {type: "submit"}}, [e("span", {staticClass: "btn-text"}, [t._v("登录")]), e("span", {staticClass: "spinner"})])
            }, function () {
                var t = this, e = t._self._c;
                return e("div", {staticClass: "divider"}, [e("span", [t._v("OR")])])
            }], G = {
                name: "ClaudePage", methods: {
                    reset() {
                        this.$router.replace({name: "reset"})
                    }, async userlogin() {
                        var t = document.querySelector(".spinner"), e = document.querySelector(".btn-text");
                        e.style.visibility = "hidden", t.style.display = "block";
                        var a = document.getElementById("username").value,
                            i = document.getElementById("password").value;
                        try {
                            const s = await v.post(`${f.apiBaseUrl}/fuclaude/login`, {username: a, password: i}),
                                n = s.data.status;
                            if (n) t.style.display = "none", e.textContent = "登录成功,跳转中...", e.style.visibility = "visible", window.open(s.data.data), e.textContent = "登录"; else {
                                t.style.display = "none", e.style.visibility = "visible";
                                var l = s.data.message;
                                alert(l)
                            }
                        } catch (s) {
                            t.style.display = "none", e.style.visibility = "visible", alert(s)
                        }
                    }, async initiateOAuth() {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/oauth2/initiate?type=Claude`);
                            var t = e;
                            console.log(t), t.data ? window.location = t.data : console.error("No redirect URL provided")
                        } catch (e) {
                            alert(e)
                        }
                    }, switchToPandora() {
                        this.$router.replace({name: "home"})
                    }
                }
            }, R = G, L = (0, o.A)(R, M, j, !1, null, "92cb46c8", null), Q = L.exports, H = function () {
                var t = this, e = t._self._c;
                return e("el-container", {attrs: {id: "navi"}}, [e("el-aside", {
                    staticClass: "sidebar",
                    attrs: {width: "240px"}
                }, [e("h2", {
                    staticClass: "site-title",
                    staticStyle: {"text-align": "center"}
                }, [t._v("Pandora Helper")]), e("el-menu", {
                    staticClass: "el-menu-vertical-demo",
                    attrs: {
                        "default-active": "accountNav",
                        "background-color": "#ffffff",
                        "text-color": "#333",
                        "active-text-color": "#fff"
                    },
                    on: {select: t.handleMenuSelect}
                }, [e("el-menu-item", {attrs: {index: "accountNav"}}, [e("span", [t._v("账号管理")])]), e("el-menu-item", {attrs: {index: "shareNav"}}, [e("span", [t._v("分享管理")])]), e("el-menu-item", {attrs: {index: "redemptionNav"}}, [e("span", [t._v("兑换码")])]), e("el-menu-item", {attrs: {index: "carNav"}}, [e("span", [t._v("停车场")])])], 1)], 1), e("el-main", {staticClass: "main-content"}, [e("el-dropdown", {
                    staticClass: "user-menu",
                    attrs: {trigger: "click"}
                }, [e("span", {staticClass: "el-dropdown-link"}, [e("el-avatar", {
                    staticClass: "user-avatar",
                    attrs: {size: 44, src: t.avatar}
                })], 1), e("el-dropdown-menu", {
                    attrs: {slot: "dropdown"},
                    slot: "dropdown"
                }, [e("el-dropdown-item", {
                    nativeOn: {
                        click: function (e) {
                            return t.showModal.apply(null, arguments)
                        }
                    }
                }, [t._v("兑换码激活")]), e("el-dropdown-item", {
                    nativeOn: {
                        click: function (e) {
                            return t.logout.apply(null, arguments)
                        }
                    }
                }, [t._v("退出登录")])], 1)], 1), e("enhanced-dialog", {
                    attrs: {
                        isVisible: t.modalVisible,
                        title: t.modalTitle
                    }, on: {close: t.closeModal, confirm: t.submitForm}
                }, t._l(t.formFields, (function (a, i) {
                    return e("form-input", {key: i, attrs: {field: a}, on: {updateValue: t.handleUpdateValue}})
                })), 1), e(t.currentComponent, {tag: "component"})], 1)], 1)
            }, K = [], X = function () {
                var t = this, e = t._self._c;
                return e("div", {staticClass: "form-group"}, [e("label", {
                    staticClass: "form-label",
                    attrs: {for: t.field.id}
                }, [t._v(t._s(t.field.label))]), "checkbox" === t.field.type && "number" === t.field.type | "text" === t.field.type | "date" === t.field.type | "password" === t.field.type ? e("input", {
                    directives: [{
                        name: "model",
                        rawName: "v-model",
                        value: t.localValue,
                        expression: "localValue"
                    }],
                    staticClass: "form-input",
                    attrs: {id: t.field.id, type: "checkbox"},
                    domProps: {checked: Array.isArray(t.localValue) ? t._i(t.localValue, null) > -1 : t.localValue},
                    on: {
                        change: function (e) {
                            var a = t.localValue, i = e.target, l = !!i.checked;
                            if (Array.isArray(a)) {
                                var s = null, n = t._i(a, s);
                                i.checked ? n < 0 && (t.localValue = a.concat([s])) : n > -1 && (t.localValue = a.slice(0, n).concat(a.slice(n + 1)))
                            } else t.localValue = l
                        }
                    }
                }) : "radio" === t.field.type && "number" === t.field.type | "text" === t.field.type | "date" === t.field.type | "password" === t.field.type ? e("input", {
                    directives: [{
                        name: "model",
                        rawName: "v-model",
                        value: t.localValue,
                        expression: "localValue"
                    }],
                    staticClass: "form-input",
                    attrs: {id: t.field.id, type: "radio"},
                    domProps: {checked: t._q(t.localValue, null)},
                    on: {
                        change: function (e) {
                            t.localValue = null
                        }
                    }
                }) : "number" === t.field.type | "text" === t.field.type | "date" === t.field.type | "password" === t.field.type ? e("input", {
                    directives: [{
                        name: "model",
                        rawName: "v-model",
                        value: t.localValue,
                        expression: "localValue"
                    }],
                    staticClass: "form-input",
                    attrs: {id: t.field.id, type: t.field.type},
                    domProps: {value: t.localValue},
                    on: {
                        input: function (e) {
                            e.target.composing || (t.localValue = e.target.value)
                        }
                    }
                }) : t._e(), "select" === t.field.type ? e("select", {
                    directives: [{
                        name: "model",
                        rawName: "v-model",
                        value: t.localValue,
                        expression: "localValue"
                    }], staticClass: "form-select", attrs: {id: t.field.id}, on: {
                        change: [function (e) {
                            var a = Array.prototype.filter.call(e.target.options, (function (t) {
                                return t.selected
                            })).map((function (t) {
                                var e = "_value" in t ? t._value : t.value;
                                return e
                            }));
                            t.localValue = e.target.multiple ? a : a[0]
                        }, function (e) {
                            return t.handleSelectChange(e)
                        }]
                    }
                }, t._l(t.field.options, (function (a) {
                    return e("option", {key: a.value, domProps: {value: a.value}}, [t._v(" " + t._s(a.text) + " ")])
                })), 0) : t._e(), "checkbox" === t.field.type ? e("div", {staticClass: "switch-wrapper"}, [e("input", {
                    directives: [{
                        name: "model",
                        rawName: "v-model",
                        value: t.localValue,
                        expression: "localValue"
                    }],
                    staticClass: "switch-input",
                    attrs: {type: "checkbox", id: t.field.id},
                    domProps: {checked: Array.isArray(t.localValue) ? t._i(t.localValue, null) > -1 : t.localValue},
                    on: {
                        change: function (e) {
                            var a = t.localValue, i = e.target, l = !!i.checked;
                            if (Array.isArray(a)) {
                                var s = null, n = t._i(a, s);
                                i.checked ? n < 0 && (t.localValue = a.concat([s])) : n > -1 && (t.localValue = a.slice(0, n).concat(a.slice(n + 1)))
                            } else t.localValue = l
                        }
                    }
                }), e("label", {
                    staticClass: "switch-label",
                    attrs: {for: t.field.id}
                }, [e("span", {staticClass: "switch-button"})])]) : t._e()])
            }, Y = [], J = {
                props: {field: Object}, data() {
                    return {localValue: this.initializeValue()}
                }, methods: {
                    initializeValue() {
                        return "select" === this.field.type && this.field.options && this.field.options.length > 0 ? this.field.value || this.field.options[0].value : this.field.value
                    }, handleSelectChange(t) {
                        const e = t.target.value;
                        this.$emit("handleSelectChange", {type: 1, field: this.field, value: e})
                    }
                }, watch: {
                    localValue(t) {
                        this.$emit("updateValue", this.field.id, t)
                    }, field: {
                        handler() {
                            this.localValue = this.initializeValue()
                        }, deep: !0, immediate: !0
                    }
                }
            }, W = J, Z = (0, o.A)(W, X, Y, !1, null, "6ff9f6db", null), tt = Z.exports, et = function () {
                var t = this, e = t._self._c;
                return e("transition", {attrs: {name: "modal-fade"}}, [t.isVisible ? e("div", {
                    staticClass: "modal-overlay",
                    on: {click: t.close}
                }, [e("div", {
                    staticClass: "modal-content", on: {
                        click: function (t) {
                            t.stopPropagation()
                        }
                    }
                }, [e("header", {staticClass: "modal-header"}, [e("h3", [t._v(t._s(t.title))]), e("button", {
                    staticClass: "close-button",
                    on: {click: t.close}
                }, [t._v("×")])]), e("main", {staticClass: "modal-body"}, [t._t("default")], 2), e("footer", {staticClass: "modal-footer"}, [e("button", {
                    staticClass: "btn btn-confirm",
                    on: {click: t.confirm}
                }, [t._v("确认")]), e("button", {
                    staticClass: "btn btn-cancel",
                    on: {click: t.close}
                }, [t._v("取消")])])])]) : t._e()])
            }, at = [], it = {
                props: {isVisible: {type: Boolean, default: !1}, title: {type: String, default: "标题"}},
                methods: {
                    close() {
                        this.$emit("close")
                    }, confirm() {
                        this.$emit("confirm")
                    }
                }
            }, lt = it, st = (0, o.A)(lt, et, at, !1, null, "d1d906d4", null), nt = st.exports, rt = function () {
                var t = this, e = t._self._c;
                return e("el-container", {
                    staticClass: "panel",
                    attrs: {id: "accountPanel"}
                }, [e("el-header", [e("h2", [t._v("账号管理")])]), e("el-main", [e("el-row", {staticClass: "search-bar"}, [e("el-col", {attrs: {span: 18}}, [e("el-input", {
                    attrs: {
                        id: "email-query",
                        placeholder: "请输入邮箱"
                    }, model: {
                        value: t.email, callback: function (e) {
                            t.email = e
                        }, expression: "email"
                    }
                }, [e("el-button", {
                    attrs: {slot: "append"},
                    on: {click: t.emailQuery},
                    slot: "append"
                }, [t._v("查询")])], 1)], 1), e("el-col", {attrs: {span: 6}}, [e("el-button", {
                    staticClass: "create-new",
                    attrs: {type: "primary"},
                    on: {
                        click: function (e) {
                            return t.showModal()
                        }
                    }
                }, [t._v("新增")]), e("enhanced-dialog", {
                    attrs: {isVisible: t.modalVisible, title: t.modalTitle},
                    on: {close: t.closeModal, confirm: t.submitForm}
                }, t._l(t.formFields, (function (a, i) {
                    return e("form-input", {key: i, attrs: {field: a}, on: {updateValue: t.handleUpdateValue}})
                })), 1)], 1)], 1), e("el-table", {
                    staticStyle: {width: "100%"},
                    attrs: {data: t.tableData}
                }, [e("el-table-column", {
                    attrs: {
                        prop: "email",
                        label: "邮箱",
                        width: "220"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "type",
                        label: "账号类型",
                        width: "120"
                    }
                }), e("el-table-column", {
                    attrs: {prop: "accessToken", label: "Access Token", width: "200"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("span", {staticClass: "ellipsis"}, [t._v(t._s(a.row.accessToken))])]
                        }
                    }])
                }), e("el-table-column", {
                    attrs: {prop: "shared", label: "共享", width: "80"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("span", {staticClass: "ellipsis"}, [t._v(t._s(1 === a.row.shared ? "✅" : "❌"))])]
                        }
                    }])
                }), e("el-table-column", {attrs: {prop: "updateTime", label: "更新时间"}}), e("el-table-column", {
                    attrs: {label: "操作", width: "400"}, scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [1 === a.row.accountType ? e("el-button", {
                                staticStyle: {
                                    "background-color": "#6fafd2",
                                    color: "white"
                                }, attrs: {size: "mini"}, on: {
                                    click: function (e) {
                                        return t.statistic(a.row.id)
                                    }
                                }
                            }, [t._v("统计")]) : t._e(), 1 === a.row.accountType ? e("el-button", {
                                attrs: {
                                    type: "info",
                                    size: "mini"
                                }, on: {
                                    click: function (e) {
                                        return t.refresh(a.row.id)
                                    }
                                }
                            }, [t._v("刷新")]) : t._e(), e("el-button", {
                                attrs: {type: "primary", size: "mini"},
                                on: {
                                    click: function (e) {
                                        return t.showShareModal(a.row.id)
                                    }
                                }
                            }, [t._v("共享")]), e("enhanced-dialog", {
                                attrs: {
                                    visible: t.modalVisible,
                                    title: "新增共享",
                                    message: ""
                                }, on: {
                                    "update:visible": function (e) {
                                        t.modalVisible = e
                                    }, confirm: function (e) {
                                        return t.submitForm()
                                    }
                                }
                            }), e("el-button", {
                                attrs: {type: "warning", size: "mini"}, on: {
                                    click: function (e) {
                                        return t.editItem(a.row.id)
                                    }
                                }
                            }, [t._v("编辑")]), e("el-button", {
                                attrs: {size: "mini", type: "danger"},
                                on: {
                                    click: function (e) {
                                        return t.showConfirmDialog(a.row.id)
                                    }
                                }
                            }, [t._v("删除")]), e("confirm-dialog", {
                                attrs: {
                                    visible: t.isDialogVisible,
                                    title: "确认删除",
                                    message: "你确定要删除这个账号吗？"
                                }, on: {
                                    "update:visible": function (e) {
                                        t.isDialogVisible = e
                                    }, confirm: function (e) {
                                        return t.handleDelete()
                                    }
                                }
                            })]
                        }
                    }])
                })], 1), e("el-pagination", {
                    attrs: {
                        "current-page": t.currentPage,
                        "page-size": 10,
                        layout: "prev, pager, next, jumper",
                        total: t.total
                    }, on: {
                        "current-change": t.handleCurrentChange, "update:currentPage": function (e) {
                            t.currentPage = e
                        }, "update:current-page": function (e) {
                            t.currentPage = e
                        }
                    }
                }), t.chartsVisible ? e("div", {staticClass: "mychart"}, [e("div", {staticClass: "chart-container"}, [e("div", {
                    ref: "chart",
                    staticStyle: {width: "600px", height: "400px"}
                }), e("el-button", {
                    attrs: {type: "primary", size: "small"},
                    on: {click: t.closeChart}
                }, [t._v("关闭")])], 1)]) : t._e()], 1)], 1)
            }, ot = [], ct = a(3944), dt = function () {
                var t = this, e = t._self._c;
                return e("transition", {attrs: {name: "zoom"}}, [t.localVisible ? e("div", {
                    staticClass: "dialog-overlay",
                    on: {
                        click: function (e) {
                            return e.target !== e.currentTarget ? null : t.handleClose.apply(null, arguments)
                        }
                    }
                }, [e("div", {staticClass: "dialog-container"}, [e("div", {staticClass: "dialog-header"}, [e("h3", [t._v(t._s(t.title))]), e("button", {
                    staticClass: "close-button",
                    on: {click: t.handleClose}
                }, [t._v("×")])]), e("div", {staticClass: "dialog-body"}, [e("i", {staticClass: "icon-warning"}), e("p", [t._v(t._s(t.message))])]), e("div", {staticClass: "dialog-footer"}, [e("button", {
                    staticClass: "btn btn-confirm",
                    on: {click: t.handleConfirm}
                }, [t._v("确定")]), e("button", {
                    staticClass: "btn btn-cancel",
                    on: {click: t.handleClose}
                }, [t._v("取消")])])])]) : t._e()])
            }, ut = [], ht = {
                props: {
                    visible: {type: Boolean, default: !1},
                    title: {type: String, default: "确认"},
                    message: {type: String, default: "你确定要执行这个操作吗？"}
                }, data() {
                    return {localVisible: this.visible}
                }, watch: {
                    visible(t) {
                        this.localVisible = t
                    }
                }, methods: {
                    handleClose() {
                        this.localVisible = !1, this.$emit("update:visible", !1)
                    }, handleConfirm() {
                        this.$emit("confirm"), this.handleClose()
                    }
                }
            }, pt = ht, mt = (0, o.A)(pt, dt, ut, !1, null, "3891bc9e", null), ft = mt.exports, gt = {
                name: "AccountPage", components: {ConfirmDialog: ft, EnhancedDialog: nt, FormInput: tt}, data() {
                    return {
                        email: "",
                        tableData: [],
                        currentPage: 1,
                        shareAddFlag: !1,
                        total: 0,
                        isDialogVisible: !1,
                        isAccDialogVisible: !1,
                        modalVisible: !1,
                        modalTitle: "新增项目",
                        currentIndex: null,
                        chart: null,
                        chartData: [],
                        chartsVisible: !1,
                        formData: {},
                        shareFormData: {},
                        formFields: [{
                            id: "name",
                            label: "账号名称",
                            type: "text",
                            value: "",
                            required: !0
                        }, {id: "email", label: "邮箱地址", type: "text", value: "", required: !0}, {
                            id: "accountType",
                            label: "账号类型",
                            type: "select",
                            value: "ChatGPT",
                            options: [{value: 1, text: "ChatGPT"}, {value: 2, text: "Claude"}],
                            required: !0
                        }, {
                            id: "userLimit",
                            label: "分享人数上限",
                            type: "text",
                            value: "",
                            required: !0
                        }, {
                            id: "accessToken",
                            label: "ACCESS_TOKEN",
                            type: "text",
                            value: "",
                            required: !0
                        }, {
                            id: "refreshToken",
                            label: "REFRESH_TOKEN",
                            type: "text",
                            value: "",
                            required: !1
                        }, {id: "shared", label: "是否共享", type: "checkbox", value: !1, required: !0}, {
                            id: "auto",
                            label: "自动加入",
                            type: "checkbox",
                            value: !1,
                            required: !0
                        }],
                        accountFields: [{
                            id: "name",
                            label: "账号名称",
                            type: "text",
                            value: "",
                            required: !0
                        }, {id: "email", label: "邮箱地址", type: "text", value: "", required: !0}, {
                            id: "accountType",
                            label: "账号类型",
                            type: "select",
                            value: "ChatGPT",
                            options: [{value: 1, text: "ChatGPT"}, {value: 2, text: "Claude"}],
                            required: !0
                        }, {
                            id: "userLimit",
                            label: "分享人数上限",
                            type: "text",
                            value: "",
                            required: !0
                        }, {
                            id: "accessToken",
                            label: "ACCESS_TOKEN",
                            type: "text",
                            value: "",
                            required: !0
                        }, {
                            id: "refreshToken",
                            label: "REFRESH_TOKEN",
                            type: "text",
                            value: "",
                            required: !1
                        }, {id: "shared", label: "是否共享", type: "checkbox", value: !1, required: !0}, {
                            id: "auto",
                            label: "自动加入",
                            type: "checkbox",
                            value: !1,
                            required: !0
                        }],
                        shareFields: [{
                            id: "username",
                            label: "用户名",
                            type: "text",
                            value: "",
                            required: !0
                        }, {id: "password", label: "密码", type: "password", value: "", required: !0}, {
                            id: "comment",
                            label: "备注",
                            type: "text",
                            value: "",
                            required: !0
                        }, {id: "expiresAt", label: "过期时间", type: "date", value: "", required: !0}]
                    }
                }, methods: {
                    initChart() {
                        this.chart && this.chart.dispose(), this.chart = ct.Ts(this.$refs.chart);
                        const t = {
                            title: {text: "GPT使用情况", left: "center"},
                            tooltip: {trigger: "axis", axisPointer: {type: "shadow"}},
                            legend: {data: ["GPT-4o", "GPT-4", "GPT-4o-mini"], top: "bottom"},
                            xAxis: {
                                type: "category",
                                data: this.chartData.map((t => t.uniqueName)),
                                axisLabel: {rotate: 45}
                            },
                            yAxis: {type: "value", name: "使用次数"},
                            series: [{
                                name: "GPT-4o",
                                type: "bar",
                                data: this.chartData.map((t => t.usage.gpt4o)),
                                itemStyle: {color: "#91cc75"}
                            }, {
                                name: "GPT-4",
                                type: "bar",
                                data: this.chartData.map((t => t.usage.gpt4)),
                                itemStyle: {color: "#5470c6"}
                            }, {
                                name: "GPT-4o-mini",
                                type: "bar",
                                data: this.chartData.map((t => t.usage.gpt4omini)),
                                itemStyle: {color: "#fac858"}
                            }]
                        };
                        this.chart.setOption(t)
                    }, async statistic(t) {
                        try {
                            this.chartsVisible = !0;
                            const e = await v.get(`${f.apiBaseUrl}/account/statistic?id=${t}`, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}});
                            e.data.status ? (this.chartData = e.data.data, this.$nextTick((() => {
                                this.initChart()
                            }))) : this.$message.error("获取统计数据失败")
                        } catch (e) {
                            console.error("Error fetching statistics:", e), this.$message.error("获取统计数据时发生错误")
                        }
                    }, closeChart() {
                        this.chartsVisible = !1, this.chart && (this.chart.dispose(), this.chart = null)
                    }, showConfirmDialog(t) {
                        this.currentIndex = t, this.isDialogVisible = !0
                    }, async fetchAccounts(t) {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/account/list?page=` + this.currentPage + "&size=10&emailAddr=" + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            e.data.status && (this.tableData = e.data.data.data, this.total = e.data.data.total)
                        } catch (e) {
                            alert(e)
                        }
                    }, handleUpdateValue(t, e) {
                        this.$set(this.formData, t, e);
                        const a = this.formFields.findIndex((e => e.id === t));
                        -1 !== a && (this.formFields[a].value = e)
                    }, showModal() {
                        this.modalTitle = "新增账号", this.currentIndex = null, this.formFields = this.accountFields, this.resetFormFields(), this.modalVisible = !0
                    }, showShareModal(t) {
                        this.shareAddFlag = !0, this.modalTitle = "新增共享", this.currentIndex = t, this.formFields = this.shareFields, this.resetFormFields(), this.modalVisible = !0
                    }, async editItem(t) {
                        this.modalTitle = "编辑账号", this.formFields = this.accountFields, this.currentIndex = t;
                        const e = await v.get(`${f.apiBaseUrl}/account/getById?id=` + t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}});
                        let a = e.data.data;
                        this.formFields.forEach((t => {
                            t.value = a[t.id], this.formData[t.id] = a[t.id]
                        })), this.modalVisible = !0
                    }, closeModal() {
                        this.currentIndex = null, this.modalVisible = !1, this.shareAddFlag = !1
                    }, async submitForm() {
                        console.log(this.formData);
                        const t = {...this.formData};
                        this.shareAddFlag ? (t.uniqueName = t.username, t.accountId = this.currentIndex, console.log(t), v.post(`${f.apiBaseUrl}/share/add`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }))) : null !== this.currentIndex ? (t.auto = t.auto ? 1 : 0, t.shared = t.shared ? 1 : 0, t.accountType = "" === t.accountType ? 1 : t.accountType, t.id = this.currentIndex, v.patch(`${f.apiBaseUrl}/account/update`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }))) : (t.auto = t.auto ? 1 : 0, t.shared = t.shared ? 1 : 0, t.accountType = "" === t.accountType ? 1 : t.accountType, v.post(`${f.apiBaseUrl}/account/add`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }))), this.fetchAccounts(""), this.closeModal()
                    }, resetFormFields() {
                        this.shareAdd = !1, this.formData = {}, this.formFields.forEach((t => {
                            const e = "checkbox" !== t.type && "";
                            t.value = e, this.formData[t.id] = e
                        }))
                    }, emailQuery() {
                        console.log(this.email), this.fetchAccounts(this.email)
                    }, handleEdit(t) {
                        console.log(t + 123123123)
                    }, handleAdd() {
                        console.log()
                    }, async handleDelete() {
                        const t = await v.delete(`${f.apiBaseUrl}/account/delete?id=` + this.currentIndex, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        t.data.status ? alert("删除成功") : alert("删除失败，请稍后重试"), this.fetchAccounts(""), this.currentIndex = null, this.isDialogVisible = !1
                    }, handleCurrentChange(t) {
                        console.log(t), this.fetchAccounts("")
                    }, async refresh(t) {
                        const e = await v.post(`${f.apiBaseUrl}/account/refresh?id=` + t, {}, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        e.data.status ? alert("刷新成功") : alert("刷新失败，请稍后重试")
                    }
                }, mounted() {
                    this.fetchAccounts("")
                }
            }, bt = gt, vt = (0, o.A)(bt, rt, ot, !1, null, "40c173ca", null), yt = vt.exports, wt = function () {
                var t = this, e = t._self._c;
                return e("el-container", {
                    staticClass: "panel",
                    attrs: {id: "sharePanel"}
                }, [e("el-header", [e("h2", [t._v("分享管理")])]), e("el-main", [e("el-row", {staticClass: "search-bar"}, [e("el-col", {attrs: {span: 18}}, [e("el-input", {
                    attrs: {
                        id: "email-query",
                        placeholder: "请输入邮箱或用户名"
                    }, model: {
                        value: t.email, callback: function (e) {
                            t.email = e
                        }, expression: "email"
                    }
                }, [e("el-button", {
                    attrs: {slot: "append"},
                    on: {click: t.emailQuery},
                    slot: "append"
                }, [t._v("查询")])], 1)], 1), e("el-col", {attrs: {span: 6}}, [e("enhanced-dialog", {
                    attrs: {
                        isVisible: t.modalVisible,
                        title: t.modalTitle
                    }, on: {close: t.closeModal, confirm: t.submitForm}
                }, t._l(t.formFields, (function (a, i) {
                    return e("form-input", {
                        key: i,
                        attrs: {field: a},
                        on: {updateValue: t.handleUpdateValue, handleSelectChange: t.handleSelectChange}
                    })
                })), 1)], 1)], 1), e("el-table", {
                    staticStyle: {width: "100%"},
                    attrs: {data: t.tableData}
                }, [e("el-table-column", {
                    attrs: {
                        prop: "uniqueName",
                        label: "用户名"
                    }
                }), e("el-table-column", {
                    attrs: {prop: "gptCarName", label: "ChatGPT账号"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("span", {
                                staticClass: "ellipsis clickable-span underlined-span",
                                attrs: {title: "点击跳转"},
                                on: {
                                    click: function (e) {
                                        return t.openChat(a.row.gptConfigId, 1)
                                    }
                                }
                            }, [t._v(t._s(a.row.gptCarName))])]
                        }
                    }])
                }), e("el-table-column", {
                    attrs: {prop: "claudeCarName", label: "Claude账号"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("span", {
                                staticClass: "ellipsis clickable-span underlined-span",
                                attrs: {title: "点击跳转"},
                                on: {
                                    click: function (e) {
                                        return t.openChat(a.row.claudeConfigId, 2)
                                    }
                                }
                            }, [t._v(t._s(a.row.claudeCarName))])]
                        }
                    }])
                }), e("el-table-column", {
                    attrs: {
                        prop: "comment",
                        label: "备注",
                        width: "120"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "expiresAt",
                        label: "过期时间",
                        width: "120"
                    }
                }), e("el-table-column", {
                    attrs: {label: "操作"}, scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("el-button", {
                                attrs: {type: "primary", size: "mini"}, on: {
                                    click: function (e) {
                                        return t.showShareModal(a.row.id)
                                    }
                                }
                            }, [t._v("激活")]), e("enhanced-dialog", {
                                attrs: {
                                    visible: t.modalVisible,
                                    title: "新增共享",
                                    message: ""
                                }, on: {
                                    "update:visible": function (e) {
                                        t.modalVisible = e
                                    }, confirm: function (e) {
                                        return t.submitForm()
                                    }
                                }
                            }), e("el-button", {
                                attrs: {type: "warning", size: "mini"}, on: {
                                    click: function (e) {
                                        return t.editItem(a.row.id)
                                    }
                                }
                            }, [t._v("编辑")]), e("el-button", {
                                attrs: {size: "mini", type: "danger"},
                                on: {
                                    click: function (e) {
                                        return t.showConfirmDialog(a.row.id)
                                    }
                                }
                            }, [t._v("删除")]), e("confirm-dialog", {
                                attrs: {
                                    visible: t.isDialogVisible,
                                    title: "确认删除",
                                    message: "你确定要删除这个账号吗？"
                                }, on: {
                                    "update:visible": function (e) {
                                        t.isDialogVisible = e
                                    }, confirm: function (e) {
                                        return t.handleDelete()
                                    }
                                }
                            })]
                        }
                    }])
                })], 1), e("el-pagination", {
                    attrs: {
                        "current-page": t.currentPage,
                        "page-size": 10,
                        layout: "prev, pager, next, jumper",
                        total: t.total
                    }, on: {
                        "current-change": t.handleCurrentChange, "update:currentPage": function (e) {
                            t.currentPage = e
                        }, "update:current-page": function (e) {
                            t.currentPage = e
                        }
                    }
                })], 1)], 1)
            }, Ct = [], _t = {
                name: "SharePage", components: {ConfirmDialog: ft, EnhancedDialog: nt, FormInput: tt}, data() {
                    return {
                        email: "",
                        tableData: [],
                        currentPage: 1,
                        total: 0,
                        isDialogVisible: !1,
                        isAccDialogVisible: !1,
                        modalVisible: !1,
                        modalTitle: "新增项目",
                        currentIndex: null,
                        activateFlag: !1,
                        accountOpts: [],
                        formData: {},
                        shareFormData: {},
                        formFields: [{
                            id: "uniqueName",
                            label: "用户名",
                            type: "text",
                            value: "",
                            required: !0,
                            readonly: "1"
                        }, {
                            id: "comment",
                            label: "备注",
                            type: "text",
                            value: "",
                            required: !0,
                            readonly: !0
                        }, {id: "expiresAt", label: "过期时间", type: "date", value: "", required: !0}],
                        accountFields: [{
                            id: "uniqueName",
                            label: "用户名",
                            type: "text",
                            value: "",
                            required: !0
                        }, {id: "comment", label: "备注", type: "text", value: "", required: !0}, {
                            id: "expiresAt",
                            label: "过期时间",
                            type: "date",
                            value: "",
                            required: !0
                        }],
                        shareFields: [{
                            id: "accountType",
                            label: "账号类型",
                            type: "select",
                            value: "ChatGPT",
                            options: [{value: 1, text: "ChatGPT"}, {value: 2, text: "Claude"}],
                            required: !0
                        }, {
                            id: "accountOptions",
                            label: "选择账号",
                            type: "select",
                            value: "",
                            options: this.accountOpts
                        }]
                    }
                }, methods: {
                    async openChat(t, e) {
                        if (null !== t) if (1 === e) {
                            const e = await v.get(`${f.apiBaseUrl}/share/getGptShare?gptConfigId=` + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            e.data.status && window.open(e.data.data)
                        } else if (2 === e) {
                            const e = await v.get(`${f.apiBaseUrl}/share/getClaudeShare?claudeConfigId=` + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            e.data.status && window.open(e.data.data)
                        }
                    }, showConfirmDialog(t) {
                        this.currentIndex = t, this.isDialogVisible = !0
                    }, async fetchItems(t) {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/share/list?page=` + this.currentPage + "&size=10&emailAddr=" + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            e.data.status && (this.tableData = e.data.data.data, this.total = e.data.data.total)
                        } catch (e) {
                            alert(e)
                        }
                    }, handleUpdateValue(t, e) {
                        this.$set(this.formData, t, e);
                        const a = this.formFields.findIndex((e => e.id === t));
                        -1 !== a && (this.formFields[a].value = e)
                    }, showModal() {
                        this.modalTitle = "新增账号", this.currentIndex = null, this.formFields = this.accountFields, this.resetFormFields(), this.modalVisible = !0
                    }, async showShareModal(t) {
                        this.shareAddFlag = !0, this.modalTitle = "激活", this.currentIndex = t, this.formFields = this.shareFields, this.resetFormFields(), this.modalVisible = !0, this.activateFlag = !0;
                        const e = await v.get(`${f.apiBaseUrl}/account/options?type=1`, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        this.accountOpts = e.data.data;
                        const a = this.shareFields.find((t => "accountOptions" === t.id));
                        a && (a.options = this.accountOpts, this.formData = {
                            accountId: parseInt(this.accountOpts[0].value),
                            id: t
                        })
                    }, async editItem(t) {
                        this.modalTitle = "编辑共享", this.formFields = this.accountFields, this.currentIndex = t;
                        const e = await v.get(`${f.apiBaseUrl}/share/getById?id=` + t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}});
                        let a = e.data.data;
                        this.formFields.forEach((t => {
                            t.value = a[t.id], this.formData[t.id] = a[t.id]
                        })), this.modalVisible = !0
                    }, closeModal() {
                        this.modalVisible = !1, this.shareAddFlag = !1
                    }, async submitForm() {
                        const t = {...this.formData};
                        if (this.activateFlag) {
                            console.log(t), t.accountId = t.accountOptions ? parseInt(t.accountOptions) : t.accountId, t.id = this.currentIndex, t.accountType = parseInt(t.accountType);
                            const e = await v.post(`${f.apiBaseUrl}/share/distribute`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                                alert(t)
                            }));
                            e.data.status ? alert("激活成功") : alert(e.data.message), this.fetchItems(""), this.currentIndex = null, this.activateFlag = !1
                        } else if (null !== this.currentIndex) {
                            t.id = this.currentIndex;
                            const e = await v.patch(`${f.apiBaseUrl}/share/update`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                                alert(t)
                            }));
                            e.data.status && alert("编辑成功")
                        }
                        this.fetchItems(""), this.closeModal()
                    }, resetFormFields() {
                        this.shareAdd = !1, this.formData = {}, this.formFields.forEach((t => {
                            const e = "checkbox" !== t.type && "";
                            t.value = e, this.formData[t.id] = e
                        }))
                    }, emailQuery() {
                        this.fetchItems(this.email)
                    }, async handleDelete() {
                        const t = await v.delete(`${f.apiBaseUrl}/share/delete?id=` + this.currentIndex, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        t.data.status ? alert("删除成功") : alert("删除失败，请稍后重试"), this.fetchItems(""), this.isDialogVisible = !1
                    }, handleCurrentChange(t) {
                        console.log(t), this.fetchItems("")
                    }, async handleSelectChange({type: t, field: e, value: a}) {
                        if (1 == t && "accountOptions" != e.id) {
                            const t = await v.get(`${f.apiBaseUrl}/account/options?type=` + a, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            this.accountOpts = t.data.data, e.value = a;
                            const i = this.shareFields.find((t => "accountOptions" === t.id));
                            i && (i.options = this.accountOpts)
                        } else console.log(e)
                    }
                }, mounted() {
                    this.fetchItems("")
                }
            }, xt = _t, It = (0, o.A)(xt, wt, Ct, !1, null, "04b21882", null), kt = It.exports, Vt = function () {
                var t = this, e = t._self._c;
                return e("el-container", {
                    staticClass: "panel",
                    attrs: {id: "redemptionPanel"}
                }, [e("el-header", [e("h2", [t._v("兑换码管理")])]), e("el-main", [e("el-row", {staticClass: "search-bar"}, [e("el-col", {attrs: {span: 18}}, [e("el-input", {
                    attrs: {
                        id: "email-query",
                        placeholder: "请输入邮箱"
                    }, model: {
                        value: t.email, callback: function (e) {
                            t.email = e
                        }, expression: "email"
                    }
                }, [e("el-button", {
                    attrs: {slot: "append"},
                    on: {click: t.emailQuery},
                    slot: "append"
                }, [t._v("查询")])], 1)], 1), e("el-col", {attrs: {span: 6}}, [e("el-button", {
                    staticClass: "create-new",
                    attrs: {type: "primary"},
                    on: {
                        click: function (e) {
                            return t.showModal()
                        }
                    }
                }, [t._v("新增")]), e("enhanced-dialog", {
                    attrs: {isVisible: t.modalVisible, title: t.modalTitle},
                    on: {close: t.closeModal, confirm: t.submitForm}
                }, t._l(t.formFields, (function (a, i) {
                    return e("form-input", {
                        key: i,
                        attrs: {field: a},
                        on: {updateValue: t.handleUpdateValue, handleSelectChange: t.handleSelectChange}
                    })
                })), 1)], 1)], 1), e("el-table", {
                    staticStyle: {width: "100%"},
                    attrs: {data: t.tableData}
                }, [e("el-table-column", {
                    attrs: {
                        prop: "email",
                        label: "邮箱"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "accountType",
                        label: "账号类型"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "code",
                        label: "兑换码"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "duration",
                        label: "兑换时长(天)"
                    }
                }), e("el-table-column", {
                    attrs: {label: "操作"}, scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("el-button", {
                                attrs: {type: "warning", size: "mini"}, on: {
                                    click: function (e) {
                                        return t.editItem(a.row.id)
                                    }
                                }
                            }, [t._v("编辑")]), e("el-button", {
                                attrs: {size: "mini", type: "danger"},
                                on: {
                                    click: function (e) {
                                        return t.showConfirmDialog(a.row.id)
                                    }
                                }
                            }, [t._v("删除")]), e("confirm-dialog", {
                                attrs: {
                                    visible: t.isDialogVisible,
                                    title: "确认删除",
                                    message: "你确定要删除这个账号吗？"
                                }, on: {
                                    "update:visible": function (e) {
                                        t.isDialogVisible = e
                                    }, confirm: function (e) {
                                        return t.handleDelete()
                                    }
                                }
                            })]
                        }
                    }])
                })], 1), e("el-pagination", {
                    attrs: {
                        "current-page": t.currentPage,
                        "page-size": 10,
                        layout: "prev, pager, next, jumper",
                        total: t.total
                    }, on: {
                        "current-change": t.handleCurrentChange, "update:currentPage": function (e) {
                            t.currentPage = e
                        }, "update:current-page": function (e) {
                            t.currentPage = e
                        }
                    }
                })], 1)], 1)
            }, Ft = [], St = (a(4114), {
                name: "SharePage", components: {ConfirmDialog: ft, EnhancedDialog: nt, FormInput: tt}, data() {
                    return {
                        email: "",
                        tableData: [],
                        currentPage: 1,
                        total: 0,
                        isDialogVisible: !1,
                        isAccDialogVisible: !1,
                        modalVisible: !1,
                        modalTitle: "新增项目",
                        currentIndex: null,
                        activateFlag: !1,
                        accountOpts: [],
                        formData: {},
                        shareFormData: {},
                        formFields: [{
                            id: "accountId",
                            label: "账号",
                            type: "select",
                            value: "",
                            required: !0,
                            readonly: "1",
                            options: []
                        }, {
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }, {id: "count", label: "兑换数量", type: "number", value: "", required: !0}],
                        accountFields: [{
                            id: "accountId",
                            label: "账号",
                            type: "select",
                            value: "",
                            required: !0,
                            readonly: "1",
                            options: []
                        }, {
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }, {id: "count", label: "兑换数量", type: "number", value: "", required: !0}],
                        shareFields: [{
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }]
                    }
                }, methods: {
                    showConfirmDialog(t) {
                        this.currentIndex = t, this.isDialogVisible = !0
                    }, async fetchItems(t) {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/redemption/list?page=` + this.currentPage + "&size=10&emailAddr=" + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            console.log(e.data.data), e.data.status && (this.tableData = e.data.data.data, this.total = e.data.data.total)
                        } catch (e) {
                            alert(e)
                        }
                    }, handleUpdateValue(t, e) {
                        this.$set(this.formData, t, e);
                        const a = this.formFields.findIndex((e => e.id === t));
                        -1 !== a && (this.formFields[a].value = e)
                    }, async showModal() {
                        this.modalTitle = "新增兑换码", this.currentIndex = null, this.formFields = this.accountFields, this.resetFormFields(), this.modalVisible = !0;
                        const t = await v.get(`${f.apiBaseUrl}/account/list`, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        }).catch((function (t) {
                            alert(t)
                        })), e = this.accountFields.find((t => "accountId" === t.id));
                        if (e) {
                            let a = [];
                            console.log(t.data.data.data);
                            for (const e of t.data.data.data) {
                                let t = e.name + "(" + (1 === e.accountType ? "ChatGPT" : "Claude") + ")", i = e.id,
                                    l = {text: t, value: i};
                                a.push(l)
                            }
                            e.options = a, console.log(e)
                        }
                        t.data.data
                    }, async editItem(t) {
                        this.modalTitle = "编辑兑换码", this.formFields = this.shareFields, this.currentIndex = t;
                        const e = await v.get(`${f.apiBaseUrl}/redemption/getById?id=` + t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}});
                        let a = e.data.data;
                        this.formFields.forEach((t => {
                            t.value = a[t.id], this.formData[t.id] = a[t.id]
                        })), this.modalVisible = !0
                    }, closeModal() {
                        this.modalVisible = !1, this.shareAddFlag = !1, this.currentIndex = null
                    }, async submitForm() {
                        const t = {...this.formData};
                        if (this.currentIndex) t.id = this.currentIndex, t.duration = parseInt(t.duration), v.patch(`${f.apiBaseUrl}/redemption/update`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        })); else {
                            console.log(t), t.accountId = parseInt(t.accountId), t.count = parseInt(t.count), t.duration = parseInt(t.duration);
                            const e = await v.post(`${f.apiBaseUrl}/redemption/add`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                                alert(t)
                            }));
                            e.data.status ? alert("新增成功") : alert(e.data.message), this.closeModal()
                        }
                        this.fetchItems(""), this.closeModal()
                    }, resetFormFields() {
                        this.shareAdd = !1, this.formData = {}, this.formFields.forEach((t => {
                            const e = "checkbox" !== t.type && "";
                            t.value = e, this.formData[t.id] = e
                        }))
                    }, emailQuery() {
                        this.fetchItems(this.email)
                    }, async handleDelete() {
                        const t = await v.delete(`${f.apiBaseUrl}/redemption/delete?id=` + this.currentIndex, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        t.data.status ? alert("删除成功") : alert("删除失败，请稍后重试"), this.fetchItems(""), this.isDialogVisible = !1
                    }, handleCurrentChange(t) {
                        console.log(t), this.fetchItems("")
                    }, async handleSelectChange({type: t, field: e, value: a}) {
                        console.log(t, e, a)
                    }
                }, mounted() {
                    this.fetchItems("")
                }
            }), At = St, Dt = (0, o.A)(At, Vt, Ft, !1, null, "62838ca8", null), Pt = Dt.exports, Bt = function () {
                var t = this, e = t._self._c;
                return e("el-container", {
                    staticClass: "panel",
                    attrs: {id: "redemptionPanel"}
                }, [e("el-header", [e("h2", [t._v("停车场")])]), e("el-main", [e("el-row", {staticClass: "search-bar"}, [e("el-col", {attrs: {span: 18}}, [e("el-input", {
                    attrs: {
                        id: "email-query",
                        placeholder: "输入车主名称查询"
                    }, model: {
                        value: t.email, callback: function (e) {
                            t.email = e
                        }, expression: "email"
                    }
                }, [e("el-button", {
                    attrs: {slot: "append"},
                    on: {click: t.emailQuery},
                    slot: "append"
                }, [t._v("查询")])], 1)], 1), e("el-col", {attrs: {span: 6}}, [e("enhanced-dialog", {
                    attrs: {
                        isVisible: t.modalVisible,
                        title: t.modalTitle
                    }, on: {close: t.closeModal, confirm: t.submitForm}
                }, t._l(t.formFields, (function (t, a) {
                    return e("form-input", {key: a, attrs: {field: t}})
                })), 1)], 1)], 1), e("el-table", {
                    staticStyle: {width: "100%"},
                    attrs: {data: t.tableData}
                }, [e("el-table-column", {
                    attrs: {
                        prop: "email",
                        label: "账号"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "type",
                        label: "账号类型"
                    }
                }), e("el-table-column", {
                    attrs: {prop: "auto", label: "自动上车"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [e("span", {staticClass: "ellipsis"}, [t._v(t._s(1 === a.row.auto ? "yes!" : "No"))])]
                        }
                    }])
                }), e("el-table-column", {
                    attrs: {
                        prop: "usernameDesc",
                        label: "车主"
                    }
                }), e("el-table-column", {
                    attrs: {
                        prop: "countDesc",
                        label: "已上车人数 / 总人数"
                    }
                }), e("el-table-column", {
                    attrs: {label: "操作", width: "300"},
                    scopedSlots: t._u([{
                        key: "default", fn: function (a) {
                            return [!0 === a.row.authorized ? e("el-button", {
                                attrs: {type: "primary", size: "mini"},
                                on: {
                                    click: function (e) {
                                        return t.showModal(a.row.id)
                                    }
                                }
                            }, [t._v("审核")]) : t._e(), e("el-button", {
                                attrs: {type: "warning", size: "mini"},
                                on: {
                                    click: function (e) {
                                        return t.contact(a.row.username)
                                    }
                                }
                            }, [t._v("联系车主")]), e("el-button", {
                                attrs: {size: "mini", type: "success"},
                                on: {
                                    click: function (e) {
                                        return t.applyCar(a.row.id)
                                    }
                                }
                            }, [t._v("我要上车")])]
                        }
                    }])
                })], 1), e("el-dialog", {
                    attrs: {
                        visible: t.auditVisible,
                        title: "审核申请",
                        width: "400px",
                        "custom-class": "modern-audit-dialog"
                    }, on: {
                        "update:visible": function (e) {
                            t.auditVisible = e
                        }
                    }
                }, [e("div", {staticClass: "audit-content"}, [e("el-checkbox-group", {
                    staticClass: "audit-checkbox-group",
                    model: {
                        value: t.auditValues, callback: function (e) {
                            t.auditValues = e
                        }, expression: "auditValues"
                    }
                }, t._l(t.auditOptions, (function (a) {
                    return e("el-checkbox", {
                        key: a.value,
                        staticClass: "audit-checkbox-item",
                        attrs: {label: a.value}
                    }, [t._v(" " + t._s(a.label) + " ")])
                })), 1)], 1), e("span", {
                    staticClass: "dialog-footer",
                    attrs: {slot: "footer"},
                    slot: "footer"
                }, [e("el-button", {
                    staticClass: "cancel-button", attrs: {type: "danger"}, on: {
                        click: function (e) {
                            return t.submitAudit(0)
                        }
                    }
                }, [t._v("拒绝")]), e("el-button", {
                    staticClass: "submit-button",
                    attrs: {type: "primary"},
                    on: {
                        click: function (e) {
                            return t.submitAudit(1)
                        }
                    }
                }, [t._v("通过")])], 1)]), e("el-pagination", {
                    attrs: {
                        "current-page": t.currentPage,
                        "page-size": 10,
                        layout: "prev, pager, next, jumper",
                        total: t.total
                    }, on: {
                        "current-change": t.handleCurrentChange, "update:currentPage": function (e) {
                            t.currentPage = e
                        }, "update:current-page": function (e) {
                            t.currentPage = e
                        }
                    }
                })], 1)], 1)
            }, Tt = [], $t = {
                name: "SharePage", components: {EnhancedDialog: nt, FormInput: tt}, data() {
                    return {
                        email: "",
                        tableData: [],
                        currentPage: 1,
                        total: 0,
                        isDialogVisible: !1,
                        isAccDialogVisible: !1,
                        modalVisible: !1,
                        modalTitle: "新增项目",
                        currentIndex: null,
                        activateFlag: !1,
                        accountOpts: [],
                        auditVisible: !1,
                        auditValues: [],
                        auditOptions: [],
                        auditCarId: null,
                        formData: {},
                        shareFormData: {},
                        formFields: [{
                            id: "accountId",
                            label: "账号",
                            type: "select",
                            value: "",
                            required: !0,
                            readonly: "1",
                            options: []
                        }, {
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }, {id: "count", label: "兑换数量", type: "number", value: "", required: !0}],
                        accountFields: [{
                            id: "accountId",
                            label: "账号",
                            type: "select",
                            value: "",
                            required: !0,
                            readonly: "1",
                            options: []
                        }, {
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }, {id: "count", label: "兑换数量", type: "number", value: "", required: !0}],
                        shareFields: [{
                            id: "duration",
                            label: "兑换时长(天)",
                            type: "number",
                            value: "",
                            required: !0,
                            readonly: !0
                        }]
                    }
                }, methods: {
                    async submitAudit(t) {
                        console.log("提交审核", this.auditCarId, this.auditValues);
                        const e = await v.post(`${f.apiBaseUrl}/car/audit`, {
                            accountId: this.auditCarId,
                            allowApply: t,
                            ids: this.auditValues
                        }, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }));
                        e.data.status ? console.log("audit success") : alert(e.data.message), this.auditVisible = !1, this.auditValues = [], this.auditCarId = null, this.fetchItems("")
                    }, async applyCar(t) {
                        const e = await v.post(`${f.apiBaseUrl}/car/apply`, {accountId: t}, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }));
                        e.data.status ? alert("申请成功") : alert(e.data.message), this.closeModal()
                    }, async fetchItems(t) {
                        try {
                            const e = await v.get(`${f.apiBaseUrl}/car/list?page=` + this.currentPage + "&size=10&owner=" + t, {
                                withCredentials: !0,
                                headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                            });
                            e.data.status && (this.tableData = e.data.data.data, this.total = e.data.data.total)
                        } catch (e) {
                            alert(e)
                        }
                    }, handleUpdateValue(t, e) {
                        this.$set(this.formData, t, e);
                        const a = this.formFields.findIndex((e => e.id === t));
                        -1 !== a && (this.formFields[a].value = e)
                    }, async showModal(t) {
                        console.log(this.auditVisible), this.auditVisible = !0, this.auditCarId = t;
                        const e = await v.get(`${f.apiBaseUrl}/car/fetchApplies?accountId=` + this.auditCarId, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        }));
                        this.auditOptions = e.data.data
                    }, async contact(t) {
                        window.open("https://linux.do/u/" + t + "/summary")
                    }, closeModal() {
                        this.modalVisible = !1, this.shareAddFlag = !1, this.currentIndex = null
                    }, async submitForm() {
                        const t = {...this.formData};
                        if (this.currentIndex) t.id = this.currentIndex, t.duration = parseInt(t.duration), v.patch(`${f.apiBaseUrl}/redemption/update`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                            alert(t)
                        })); else {
                            console.log(t), t.accountId = parseInt(t.accountId), t.count = parseInt(t.count), t.duration = parseInt(t.duration);
                            const e = await v.post(`${f.apiBaseUrl}/redemption/add`, t, {headers: {Authorization: "Bearer " + localStorage.getItem("token")}}).catch((function (t) {
                                alert(t)
                            }));
                            e.data.status ? alert("新增成功") : alert(e.data.message), this.closeModal()
                        }
                        this.fetchItems(""), this.closeModal()
                    }, resetFormFields() {
                        this.shareAdd = !1, this.formData = {}, this.formFields.forEach((t => {
                            const e = "checkbox" !== t.type && "";
                            t.value = e, this.formData[t.id] = e
                        }))
                    }, emailQuery() {
                        this.fetchItems(this.email)
                    }, handleCurrentChange(t) {
                        console.log(t), this.fetchItems("")
                    }, async handleSelectChange({type: t, field: e, value: a}) {
                        console.log(t, e, a)
                    }
                }, mounted() {
                    this.fetchItems("")
                }
            }, qt = $t, Ut = (0, o.A)(qt, Bt, Tt, !1, null, "35352db6", null), zt = Ut.exports, Ot = {
                components: {EnhancedDialog: nt, FormInput: tt}, name: "NaviPage", data() {
                    return {
                        avatar: "/linuxdo.webp",
                        activeMenu: "accountNav",
                        currentComponent: yt,
                        modalVisible: !1,
                        itemData: "",
                        modalTitle: "",
                        formFields: [{id: "code", label: "兑换码", type: "text", value: "", required: !0}]
                    }
                }, methods: {
                    loadAvatar() {
                        const t = localStorage.getItem("img");
                        t && (this.avatar = t)
                    }, handleUpdateValue(t, e) {
                        this.itemData = e;
                        const a = this.formFields.findIndex((e => e.id === t));
                        -1 !== a && (this.formFields[a].value = e)
                    }, showModal() {
                        console.log(111), this.modalTitle = "兑换码激活", this.resetFormFields(), this.modalVisible = !0
                    }, closeModal() {
                        this.modalVisible = !1
                    }, async submitForm() {
                        console.log(this.itemData);
                        const t = await v.get(`${f.apiBaseUrl}/redemption/activate?code=` + this.itemData, {
                            withCredentials: !0,
                            headers: {Authorization: "Bearer " + localStorage.getItem("token")}
                        });
                        t.data.status ? alert("删除成功") : alert(t.data.message)
                    }, resetFormFields() {
                        this.formData = {}, this.formFields.forEach((t => {
                            const e = "checkbox" !== t.type && "";
                            t.value = e, this.formData[t.id] = e
                        }))
                    }, handleMenuSelect(t) {
                        switch (this.activeMenu = t, t) {
                            case"accountNav":
                                this.currentComponent = yt;
                                break;
                            case"shareNav":
                                this.currentComponent = kt;
                                break;
                            case"redemptionNav":
                                this.currentComponent = Pt;
                                break;
                            case"carNav":
                                this.currentComponent = zt;
                                break;
                            default:
                                this.currentComponent = kt
                        }
                    }, logout() {
                        this.$router.replace({name: "home"}), localStorage.removeItem("token"), localStorage.removeItem("img")
                    }
                }, created() {
                    this.loadAvatar()
                }
            }, Et = Ot, Nt = (0, o.A)(Et, H, K, !1, null, "8d2fe03c", null), Mt = Nt.exports;
            i["default"].use(u.Ay);
            const jt = [{path: "/", component: _, name: "home", meta: {title: "首页"}}, {
                path: "/account",
                component: yt,
                name: "account",
                meta: {title: "账号管理"}
            }, {path: "/share", component: kt, name: "share", meta: {title: "分享管理"}}, {
                path: "/redemption",
                component: Pt,
                name: "redemption",
                meta: {title: "兑换码"}
            }, {path: "/car", component: zt, name: "car", meta: {title: "停车场"}}, {
                path: "/reset",
                component: S,
                name: "reset",
                meta: {title: "重置密码"}
            }, {path: "/pandora", component: $, name: "pandora", meta: {title: "Pandora"}}, {
                path: "/loading",
                component: N,
                name: "loading",
                meta: {title: "Loading"}
            }, {path: "/claude", component: Q, name: "claude", meta: {title: "Fuclaude"}}, {
                path: "/navi",
                component: Mt,
                name: "navi",
                meta: {title: "Helper"}
            }], Gt = new u.Ay({mode: "history", routes: jt});

            function Rt(t) {
                let e = document.querySelector("link[rel~='icon']");
                e || (e = document.createElement("link"), e.rel = "icon", document.getElementsByTagName("head")[0].appendChild(e)), e.href = t, e.type = "iamge/svg+xml"
            }

            Gt.beforeEach(((t, e, a) => {
                t.meta.title ? (document.title = t.meta.title, Rt("humbleicons--coffee.svg")) : document.title = "Pandora", a()
            }));
            var Lt = Gt, Qt = a(2457), Ht = a.n(Qt);
            i["default"].config.productionTip = !1, i["default"].use(Ht()), new i["default"]({
                render: t => t(d),
                router: Lt
            }).$mount("#app")
        }, 8902: function (t, e, a) {
            t.exports = a.p + "img/ph--user-switch.8dbf8a86.svg"
        }, 4832: function (t, e, a) {
            t.exports = a.p + "img/linuxdo.7bff183f.webp"
        }
    }, e = {};

    function a(i) {
        var l = e[i];
        if (void 0 !== l) return l.exports;
        var s = e[i] = {id: i, loaded: !1, exports: {}};
        return t[i].call(s.exports, s, s.exports, a), s.loaded = !0, s.exports
    }

    a.m = t, function () {
        a.amdO = {}
    }(), function () {
        var t = [];
        a.O = function (e, i, l, s) {
            if (!i) {
                var n = 1 / 0;
                for (d = 0; d < t.length; d++) {
                    i = t[d][0], l = t[d][1], s = t[d][2];
                    for (var r = !0, o = 0; o < i.length; o++) (!1 & s || n >= s) && Object.keys(a.O).every((function (t) {
                        return a.O[t](i[o])
                    })) ? i.splice(o--, 1) : (r = !1, s < n && (n = s));
                    if (r) {
                        t.splice(d--, 1);
                        var c = l();
                        void 0 !== c && (e = c)
                    }
                }
                return e
            }
            s = s || 0;
            for (var d = t.length; d > 0 && t[d - 1][2] > s; d--) t[d] = t[d - 1];
            t[d] = [i, l, s]
        }
    }(), function () {
        a.n = function (t) {
            var e = t && t.__esModule ? function () {
                return t["default"]
            } : function () {
                return t
            };
            return a.d(e, {a: e}), e
        }
    }(), function () {
        a.d = function (t, e) {
            for (var i in e) a.o(e, i) && !a.o(t, i) && Object.defineProperty(t, i, {enumerable: !0, get: e[i]})
        }
    }(), function () {
        a.g = function () {
            if ("object" === typeof globalThis) return globalThis;
            try {
                return this || new Function("return this")()
            } catch (t) {
                if ("object" === typeof window) return window
            }
        }()
    }(), function () {
        a.o = function (t, e) {
            return Object.prototype.hasOwnProperty.call(t, e)
        }
    }(), function () {
        a.r = function (t) {
            "undefined" !== typeof Symbol && Symbol.toStringTag && Object.defineProperty(t, Symbol.toStringTag, {value: "Module"}), Object.defineProperty(t, "__esModule", {value: !0})
        }
    }(), function () {
        a.nmd = function (t) {
            return t.paths = [], t.children || (t.children = []), t
        }
    }(), function () {
        a.p = "/"
    }(), function () {
        var t = {524: 0};
        a.O.j = function (e) {
            return 0 === t[e]
        };
        var e = function (e, i) {
            var l, s, n = i[0], r = i[1], o = i[2], c = 0;
            if (n.some((function (e) {
                return 0 !== t[e]
            }))) {
                for (l in r) a.o(r, l) && (a.m[l] = r[l]);
                if (o) var d = o(a)
            }
            for (e && e(i); c < n.length; c++) s = n[c], a.o(t, s) && t[s] && t[s][0](), t[s] = 0;
            return a.O(d)
        }, i = self["webpackChunkpandora_helper"] = self["webpackChunkpandora_helper"] || [];
        i.forEach(e.bind(null, 0)), i.push = e.bind(null, i.push.bind(i))
    }();
    var i = a.O(void 0, [504], (function () {
        return a(6547)
    }));
    i = a.O(i)
})();
//# sourceMappingURL=app.715c671f.js.map