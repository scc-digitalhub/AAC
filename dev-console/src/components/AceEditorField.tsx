import { useRecordContext, FieldProps, useTranslate } from 'react-admin';
import AceEditor from 'react-ace';

import 'ace-builds/src-noconflict/mode-java';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/mode-markdown';
import 'ace-builds/src-noconflict/mode-drools';
import 'ace-builds/src-noconflict/mode-html';
import 'ace-builds/src-noconflict/mode-python';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/mode-sql';
import 'ace-builds/src-noconflict/mode-typescript';
import 'ace-builds/src-noconflict/mode-css';
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/mode-text';

import 'ace-builds/src-noconflict/theme-github';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-solarized_dark';
import 'ace-builds/src-noconflict/theme-solarized_light';
import { Fragment } from 'react';
import { get } from 'lodash';

const AceEditorField = (props: AceFieldProps) => {
    const {
        mode = 'html',
        parse = data => data,
        theme = 'github',
        fullWidth = false,
        minLines = 5,
        maxLines = 25,
        width = '50vw',
        source,
    } = props;
    const record = useRecordContext(props);
    const value = get(record, source) || '';

    const aceOptions = {
        readOnly: true,
        useWorker: false,
        showPrintMargin: false,
        minLines,
        maxLines,
    };

    return (
        <Fragment>
            <AceEditor
                value={parse(value)}
                mode={mode}
                theme={theme}
                wrapEnabled
                width={fullWidth ? '100%' : width}
                setOptions={aceOptions}
            />
        </Fragment>
    );
};

export interface AceFieldProps extends FieldProps {
    source: string;
    parse?: (any) => string | null;
    mode?:
        | 'java'
        | 'javascript'
        | 'markdown'
        | 'drools'
        | 'html'
        | 'python'
        | 'json'
        | 'sql'
        | 'typescript'
        | 'css'
        | 'yaml'
        | 'text';
    fullWidth?: boolean;
    minLines?: number;
    maxLines?: number;
    width?: string;
    theme?: 'github' | 'monokai' | 'solarized_dark' | 'solarized_light';
}
