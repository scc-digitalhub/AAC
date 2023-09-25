import {
    ArrayField,
    ChipField,
    Datagrid,
    List,
    SingleFieldList,
    TextField,
} from 'react-admin';

export const MetricsList = () => (
    <List>
        <Datagrid rowClick="edit">
            <TextField source="id" />
            <TextField source="name" />
            <TextField source="description" />
            <TextField source="baseUnit" />
            {/* <ArrayField source="measurements">
                <SingleFieldList>
                    <ChipField source="statistic" />
                </SingleFieldList>
            </ArrayField> */}
            <ArrayField source="availableTags">
                <SingleFieldList>
                    <ChipField source="tag" />
                </SingleFieldList>
            </ArrayField>
            <TextField source="sample.value" />
        </Datagrid>
    </List>
);
