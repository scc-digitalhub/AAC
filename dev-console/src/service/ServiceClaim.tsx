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

export const ClaimEditForm = (props: { mode: 'create' | 'edit' }) => {
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
                    text={translate('page.service.claim.definition.title')}
                    secondaryText={translate(
                        'page.service.claim.definition.subtitle'
                    )}
                />
                <Grid container gap={1} mb={2} alignContent={'baseline'}>
                    <Grid item xs={12} md={5}>
                        <TextInput
                            source="key"
                            label="field.key.name"
                            helperText="field.key.helperText"
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
                    <Grid item xs={12} md={5}>
                        <TextInput
                            source="name"
                            label="field.name.name"
                            helperText="field.name.helperText"
                            required
                            fullWidth
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
                    <Grid item xs={12} md={3}>
                        <SelectInput
                            source="type"
                            label="field.type.name"
                            helperText="field.type.helperText"
                            choices={[
                                { id: 'string', name: 'string' },
                                { id: 'number', name: 'number' },
                                { id: 'date', name: 'date' },
                                { id: 'boolean', name: 'boolean' },
                                { id: 'object', name: 'object' },
                            ]}
                            required
                            sx={{ marginTop: 0 }}
                        />
                    </Grid>

                    <Grid item xs={12}>
                        <BooleanInput
                            source="multiple"
                            label="field.multiple.name"
                            helperText="field.multiple.helperText"
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
