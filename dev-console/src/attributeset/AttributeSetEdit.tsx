import { Box, Chip, Stack } from '@mui/material';
import {
    ArrayField,
    ArrayInput,
    BooleanInput,
    ChipField,
    Edit,
    Labeled,
    NumberField,
    NumberInput,
    ReferenceArrayInput,
    regex,
    required,
    SelectInput,
    SimpleFormIterator,
    TabbedForm,
    TextField,
    TextInput,
    TopToolbar,
    useGetList,
    useRecordContext,
    useTranslate,
    WrapperField,
} from 'react-admin';
import { InspectButton } from '@dslab/ra-inspect-button';
import { DeleteWithDialogButton } from '@dslab/ra-delete-dialog-button';
import { IdField } from '../components/IdField';
import { Page } from '../components/Page';
import { TabToolbar } from '../components/TabToolbar';
import { DatagridArrayInput } from '@dslab/ra-datagrid-input';
import { SectionTitle } from '../components/sectionTitle';
import { RefreshingExportButton } from '../components/RefreshingExportButton';
import { ResourceTitle } from '../components/ResourceTitle';
import { AttributeEditForm, ClaimEditForm } from '../service/ServiceClaim';

const pattern = /^[a-zA-Z.:_-]{3,}$/;

export const AttributeSetEdit = () => {
    return (
        <Page>
            <Edit
                actions={<AttributeSetToolBarActions />}
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
                    <TextField source="id" />
                </Labeled>
                <Labeled>
                    <TextField source="name" />
                </Labeled>
                <Labeled>
                    <TextField source="identifier" />
                </Labeled>
                <Labeled label="keys">
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
                    text={translate('page.attributeSet.settings.header.title')}
                    secondaryText={translate(
                        'page.attributeSet.settings.header.subtitle'
                    )}
                />
                <TextInput source="identifier" fullWidth readOnly />
                <TextInput source="name" fullWidth />
                <TextInput source="description" multiline fullWidth />
            </TabbedForm.Tab>

            {record?.identifier && !record.identifier.startsWith('aac') && (
                <TabbedForm.Tab label="tab.attributes">
                    <SectionTitle
                        text={translate(
                            'page.attributeSet.attributes.header.title'
                        )}
                        secondaryText={translate(
                            'page.attributeSet.attributes.header.subtitle'
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

const AttributeSetToolBarActions = () => {
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
