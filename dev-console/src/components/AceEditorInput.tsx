import AceEditor from 'react-ace';
import 'ace-builds/src-noconflict/mode-javascript';
import 'ace-builds/src-noconflict/mode-json';
import 'ace-builds/src-noconflict/mode-yaml';
import 'ace-builds/src-noconflict/mode-css';
import 'ace-builds/src-noconflict/mode-text';
import 'ace-builds/src-noconflict/theme-github';
import 'ace-builds/src-noconflict/theme-monokai';
import 'ace-builds/src-noconflict/theme-solarized_light';
import { Ace, config, version } from 'ace-builds';
import { AceInputProps } from '@dslab/ra-ace-editor';
import { useInput, Labeled, InputHelperText } from 'react-admin';

/**
 * Local copy of @dslab/ra-ace-editor AceEditorInput exposing `onLoad`.
 * Disables Ace's `overwrite` command (bound to Insert), which otherwise
 * turns the editor into overtype mode.
 */
export const AceEditorInput = (
    props: AceInputProps & { onLoad?: (editor: Ace.Editor) => void }
) => {
    const {
        mode = 'html',
        theme = 'monokai',
        useWorker = false,
        format = data => data,
        parse = data => data,
        basePath = 'https://cdn.jsdelivr.net/npm/ace-builds@',
        label,
        helperText,
        fullWidth = false,
        minLines = 5,
        maxLines = 25,
        width = '50vw',
        onBlur,
        onChange,
        onLoad,
        resource,
        source,
        disabled = false,
        ...rest
    } = props;

    const {
        id,
        field,
        fieldState: { isTouched, error },
        formState: { isSubmitted },
        isRequired,
    } = useInput({ resource, source, onChange, onBlur, disabled, ...rest });

    const value = field ? parse(field.value || '') : '';

    const handleLoad = (editor: Ace.Editor) => {
        editor.commands.removeCommand('overwrite');
        onLoad?.(editor);
    };

    config.set('basePath', basePath + version + '/src-noconflict/');

    return (
        <>
            <Labeled
                {...{ fullWidth, isRequired, label, resource, source }}
                id={id}
            >
                <AceEditor
                    mode={disabled ? 'text' : mode}
                    value={value}
                    onChange={data => field.onChange(format(data))}
                    onLoad={handleLoad}
                    theme={theme}
                    wrapEnabled
                    width={fullWidth ? '100%' : width}
                    setOptions={{
                        useWorker,
                        showPrintMargin: false,
                        readOnly: disabled,
                        minLines,
                        maxLines,
                    }}
                />
            </Labeled>
            <InputHelperText
                touched={isTouched || isSubmitted}
                error={error?.message}
                helperText={helperText}
            />
        </>
    );
};
