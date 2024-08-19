import { Grid } from '@mui/material';
import {
    Button,
    Labeled,
    useDataProvider,
    useNotify,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { claimMappingDefaultValue } from './schemas';
import utils from '../utils';
import { ControlledEditorInput } from '../components/ControllerEditorInput';
import { useWatch } from 'react-hook-form';
import { useRootSelector } from '@dslab/ra-root-selector';
import { useEffect, useState } from 'react';
import TestIcon from '@mui/icons-material/DirectionsRun';
import { AceEditorField } from '../components/AceEditorField';

export const ClaimMappingEditor = () => {
    const dataProvider = useDataProvider();
    const { root: realmId } = useRootSelector();
    const record = useRecordContext();
    const notify = useNotify();
    const translate = useTranslate();
    const code = useWatch({
        name: 'hookFunctions.claimMapping',
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
        if (code && record && realmId) {
            dataProvider
                .invoke({
                    path: 'apps/' + realmId + '/' + record.id + '/claims',
                    body: JSON.stringify({
                        code,
                        name: 'claimMapping',
                        scopes: [],
                    }),
                    options: {
                        method: 'POST',
                    },
                })
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
                source="hookFunctions.claimMapping"
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
