import { Grid } from '@mui/material';
import {
    Button,
    Labeled,
    useNotify,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import utils from '../utils';
import { ControlledEditorInput } from '../components/ControllerEditorInput';
import { useWatch } from 'react-hook-form';
import { useEffect, useState } from 'react';
import TestIcon from '@mui/icons-material/DirectionsRun';
import { AceEditorField } from '@dslab/ra-ace-editor';

export const ClaimMappingEditor = (props: {
    source: string;
    onTest: (record, code) => Promise<any>;
    defaultValue: string;
    label?: string;
    helperText?: string;
}) => {
    const {
        source,
        onTest,
        defaultValue: claimMappingDefaultValue,
        label,
        helperText,
    } = props;
    const record = useRecordContext();
    const notify = useNotify();
    const translate = useTranslate();
    const code = useWatch({
        name: source,
        defaultValue: null,
    });
    const defaultValue = {
        context: null,
        result: null,
        errors: [],
        messages: null,
    };

    const [result, setResult] = useState<any>(defaultValue);

    useEffect(() => {
        if (code == null) {
            setResult(defaultValue);
        }
    }, [code]);

    const handleTest = e => {
        if (code && record) {
            onTest(record, code)
                .then(json => {
                    if (json) {
                        const res = {
                            ...json,
                            context: json.context
                                ? JSON.stringify(json.context, null, 2)
                                : null,
                            result: json.result
                                ? JSON.stringify(json.result, null, 4)
                                : null,
                        };

                        setResult(res);
                        if (json.errors.length > 0) {
                            notify(json.errors[0], { type: 'error' });
                        }
                    } else {
                        notify('ra.notification.bad_item', { type: 'warning' });
                    }
                })
                .catch(error => {
                    const msg = error.message || 'ra.notification.error';
                    notify(msg, { type: 'error' });
                });
        }
    };

    return (
        <>
            <ControlledEditorInput
                source={source}
                label={label}
                helperText={helperText}
                defaultValue={claimMappingDefaultValue}
                mode="javascript"
                parse={utils.parseBase64}
                format={utils.encodeBase64}
                minLines={25}
            />
            <Button
                disabled={!code}
                onClick={handleTest}
                autoFocus
                variant="contained"
                size="large"
                color="success"
                startIcon={<TestIcon />}
                label={translate('action.test')}
                sx={{ mb: 3 }}
            />

            <Grid container gap={1} mb={2}>
                <Grid item xs={12} md={5}>
                    {result?.context && (
                        <Labeled label="field.context" width="100%">
                            <AceEditorField
                                record={result}
                                source="context"
                                mode="json"
                                theme="solarized_light"
                                width="100%"
                                minLines={25}
                                maxLines={35}
                            />
                        </Labeled>
                    )}
                </Grid>
                <Grid item xs={12} md={5}>
                    {result?.result && (
                        <Labeled label="field.result" width="100%">
                            <AceEditorField
                                record={result}
                                source="result"
                                mode="json"
                                theme="solarized_light"
                                width="100%"
                                minLines={25}
                                maxLines={35}
                            />
                        </Labeled>
                    )}
                </Grid>
            </Grid>
        </>
    );
};
