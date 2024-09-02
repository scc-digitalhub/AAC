import { Box, Chip, Stack } from '@mui/material';
import {
    ArrayInput,
    BooleanInput,
    Edit,
    Labeled,
    regex,
    required,
    SaveButton,
    SelectInput,
    SimpleFormIterator,
    TabbedForm,
    TextField,
    TextInput,
    Toolbar,
    TopToolbar,
    useRecordContext,
    useTranslate,
    WrapperField,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { Page } from '../components/Page';
import { SectionTitle } from '../components/SectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';

const pattern = /^[a-zA-Z.:_-]{3,}$/;

export const AttributeSetEdit = () => {
    return (
        <Page>
            <Edit
                actions={<ActionsToolbar />}
                mutationMode="optimistic"
                component={Box}
                redirect={'edit'}
            >
                <ResourceTitle />
                <AttributeSetEditForm />
            </Edit>
        </Page>
    );
};

const AttributeSetEditForm = () => {
    const translate = useTranslate();
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TabbedForm toolbar={<TabToolbar />} syncWithLocation={false}>
            <TabbedForm.Tab label="tab.overview">
                <Labeled>
                    <TextField source="id" label="field.id.name" />
                </Labeled>
                <Labeled>
                    <TextField source="name" label="field.name.name" />
                </Labeled>
                <Labeled>
                    <TextField
                        source="identifier"
                        label="field.identifier.name"
                    />
                </Labeled>
                <Labeled label="field.keys.name">
                    <WrapperField label="keys">
                        <Stack direction={'row'} flexWrap={'wrap'} gap={1}>
                            {record?.keys?.map(k => (
                                <Chip label={k} key={'k-' + k} />
                            ))}
                        </Stack>
                    </WrapperField>
                </Labeled>
            </TabbedForm.Tab>
            <TabbedForm.Tab label="tab.settings">
                <SectionTitle
                    text={translate('page.attributeset.settings.header.title')}
                    secondaryText={translate(
                        'page.attributeset.settings.header.subtitle'
                    )}
                />
                <TextInput
                    source="identifier"
                    label="field.identifier.name"
                    helperText="field.identifier.helperText"
                    fullWidth
                    readOnly
                />
                <TextInput
                    source="name"
                    label="field.name.name"
                    helperText="field.name.helperText"
                    fullWidth
                />
                <TextInput
                    source="description"
                    label="field.description.name"
                    helperText="field.description.helperText"
                    multiline
                    fullWidth
                />
            </TabbedForm.Tab>

            {record?.identifier && !record.identifier.startsWith('aac') && (
                <TabbedForm.Tab label="tab.attributes">
                    <SectionTitle
                        text={translate(
                            'page.attributeset.attributes.header.title'
                        )}
                        secondaryText={translate(
                            'page.attributeset.attributes.header.subtitle'
                        )}
                    />

                    <ArrayInput source="attributes">
                        <SimpleFormIterator>
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
                            />
                            <TextInput
                                source="name"
                                label="field.name.name"
                                helperText="field.name.helperText"
                                required
                                fullWidth
                            />
                            <TextInput
                                source="description"
                                label="field.description.name"
                                helperText="field.description.helperText"
                                fullWidth
                                multiline
                            />
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
                            <BooleanInput
                                source="multiple"
                                label="field.multiple.name"
                                helperText="field.multiple.helperText"
                            />
                        </SimpleFormIterator>
                    </ArrayInput>
                </TabbedForm.Tab>
            )}
        </TabbedForm>
    );
};

const ActionsToolbar = () => {
    const record = useRecordContext();
    if (!record) return null;

    return (
        <TopToolbar>
            <InspectButton />
            <DeleteWithDialogButton />
            <RefreshingExportButton />
        </TopToolbar>
    );
};

const TabToolbar = () => (
    <Toolbar>
        <SaveButton />
    </Toolbar>
);
