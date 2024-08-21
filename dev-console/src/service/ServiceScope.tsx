import {
    BooleanInput,
    Form,
    ReferenceArrayInput,
    regex,
    required,
    SaveButton,
    SelectInput,
    TextInput,
    useRecordContext,
    useTranslate,
} from 'react-admin';
import { DialogActions, Grid, Stack } from '@mui/material';
import { Page } from '../components/Page';
import { SectionTitle } from '../components/sectionTitle';
import { useRootSelector } from '@dslab/ra-root-selector';
import utils from '../utils';
import { ControlledEditorInput } from '../components/ControllerEditorInput';

const scopeAuthorizationDefaultValue =
    'LyoqCiAqIERFRklORSBZT1VSIE9XTiBBUFBST1ZBTCBGVU5DVElPTiBIRVJFCiAqIGlucHV0IGlzIGEgbWFwIGNvbnRhaW5pbmcgdXNlciwgY2xpZW50LCBhbmQgc2NvcGVzCioqLwpmdW5jdGlvbiBhcHByb3ZlcihpbnB1dERhdGEpIHsKICAgcmV0dXJuIHthcHByb3ZlZDogZmFsc2UsIGV4cGlyZXNBdDogbnVsbH07Cn0';

export const ScopeEditForm = (props: { mode: 'create' | 'edit' }) => {
    const { mode } = props;
    const translate = useTranslate();
    const record = useRecordContext();
    const { root: realmId } = useRootSelector();
    if (!record) return null;
    const serviceId = record.serviceId;
    const pattern = /^[a-zA-Z.:_-]{3,}$/;

    return (
        <Form>
            <Page>
                <SectionTitle
                    text={translate('page.service.scope.definition.title')}
                    secondaryText={translate(
                        'page.service.scope.definition.subtitle'
                    )}
                />
                <Grid container gap={1} mb={2} alignContent={'baseline'}>
                    <Grid item xs={12} md={3}>
                        <TextInput
                            source="scope"
                            label="field.scope.name"
                            helperText="field.scope.helperText"
                            fullWidth
                            validate={[
                                required(),
                                regex(
                                    pattern,
                                    translate('ra.validation.regex', {
                                        pattern,
                                    })
                                ),
                            ]}
                            readOnly={mode === 'edit'}
                        />
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <TextInput
                            source="name"
                            label="field.name.name"
                            helperText="field.name.helperText"
                            required
                            fullWidth
                        />
                    </Grid>
                    <Grid item xs={12} md={3}>
                        <SelectInput
                            source="type"
                            label="field.authority.name"
                            helperText="field.authority.helperText"
                            choices={[
                                { id: 'generic', name: 'generic' },
                                { id: 'user', name: 'user' },
                                { id: 'client', name: 'client' },
                            ]}
                            required
                            defaultValue={'generic'}
                            sx={{ marginTop: 0 }}
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <TextInput
                            source="description"
                            label="field.description.name"
                            helperText="field.description.helperText"
                            fullWidth
                            multiline
                        />
                    </Grid>

                    <Grid item xs={12}>
                        <ReferenceArrayInput
                            source="claims"
                            reference={
                                'services/' +
                                realmId +
                                '/' +
                                serviceId +
                                '/claims'
                            }
                            label="field.approvalRoles.name"
                            helperText="field.approvalRoles.helperText"
                        />
                    </Grid>
                </Grid>
                <SectionTitle
                    text={translate('page.service.scope.authorizations.title')}
                    secondaryText={translate(
                        'page.service.scope.authorizations.subtitle'
                    )}
                />
                <Grid container gap={1}>
                    <Grid item xs={12}>
                        <BooleanInput
                            source="approvalAny"
                            label="field.approvalAny.name"
                            helperText="field.approvalAny.helperText"
                        />
                        <BooleanInput
                            source="approvalRequired"
                            label="field.approvalRequired.name"
                            helperText="field.approvalRequired.helperText"
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <ReferenceArrayInput
                            source="approvalRoles"
                            reference="roles"
                            label="field.approvalRoles.name"
                            helperText="field.approvalRoles.helperText"
                        />
                    </Grid>
                    <Grid item xs={12}>
                        <ControlledEditorInput
                            source="approvalFunction"
                            defaultValue={scopeAuthorizationDefaultValue}
                            disabledValue={null}
                            mode="javascript"
                            parse={utils.parseBase64}
                            format={utils.encodeBase64}
                            minLines={10}
                        />
                    </Grid>
                </Grid>
            </Page>
            <DialogActions>
                <SaveButton variant="text" alwaysEnable />
            </DialogActions>
        </Form>
    );
};
