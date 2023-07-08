"use strict";
import { TmLanguage } from "./TMLanguageModel";

export const rclangTmLanguage: TmLanguage = {
    fileTypes: [
        'rc-lang'
    ],
    firstLineMatch: '^#!/.*\\b\\w*rclang\\b',
    foldingStartMarker: '/\\*\\*|\\{\\s*$',
    foldingStopMarker: '\\*\\*/|^\\s*\\}',
    keyEquivalent: '^~S',
    repository: {
        keywords: {
            patterns: [
                {
                    match: '\\b(return)\\b',
                    name: 'keyword.control.flow.jump.rclang'
                },
                {
                    match: '\\b(if|then|elsif|else|while|for|break|continue)\\b',
                    name: 'keyword.control.flow.rclang'
                },
                {
                    match: '\\b(end)\\b',
                    name: 'keyword.declaration.end.rclang'
                },
                {
                    match: '\\b(class)\\b',
                    name: 'keyword.declaration.class.rclang'
                },
                {
                    match: '\\b(var|val|def)\\b',
                    name: 'keyword.declaration.begin.rclang'
                },
                {
                    match: '\\b(import)\\b',
                    name: 'keyword.other.rclang'
                },
            ]
        },
        strings: {
            patterns: [
                {
                    match: '".*"',
                    name: 'constant.literal.rclang'
                }
            ]
        },
        constants: {
            patterns: [
                {
                    match: '\\b(false|true)\\b',
                    name: 'constant.language.rclang'
                },
                {
                    match: '\\b(0[xX][0-9a-fA-F_]*)\\b',
                    name: 'constant.numeric.rclang'
                },
                {
                    match: '\\b(([0-9][0-9_]*(\\.[0-9][0-9_]*)?)([eE](\\+|-)?[0-9][0-9_]*)?|[0-9][0-9_]*)[LlFfDd]?\\b',
                    name: 'constant.numeric.rclang'
                },
                {
                    match: '(\\.[0-9][0-9_]*)([eE](\\+|-)?[0-9][0-9_]*)?[LlFfDd]?\\b',
                    name: 'constant.numeric.rclang'
                },
                {
                    match: '\\b(self|super)\\b',
                    name: 'variable.language.rclang'
                }
            ]
        },
        code: {
            patterns: [
                {
                    include: '#keywords'
                },
                {
                    include: '#strings'
                },
                {
                    include: '#constants'
                },
            ]
        }
    },
    patterns: [
        {
            include: '#code'
        }
    ],
    name: 'rclang',
    scopeName: "source.rc",
}