import {
    Link,
    useCreatePath,
    useGetResourceLabel,
    useRecordContext,
    useResourceContext,
    useResourceDefinition,
} from 'react-admin';
import { PageTitle } from '../components/PageTitle';
import BlankIcon from '@mui/icons-material/CheckBoxOutlineBlank';
import { isValidElement, ReactElement } from 'react';
import { Typography } from '@mui/material';

export const ResourceTitle = (props: {
    text?: string | ReactElement;
    icon?: ReactElement;
    breadcrumb?: ReactElement | boolean;
}) => {
    const { text, icon, breadcrumb = true } = props;
    const record = useRecordContext();
    const resource = useResourceContext();
    const definition = useResourceDefinition();
    const createPath = useCreatePath();
    const getResourceLabel = useGetResourceLabel();

    if (!record || !definition) {
        return null;
    }

    const ResourceIcon = definition.icon || BlankIcon;
    const iconEl =
        icon && isValidElement(icon) ? (
            icon
        ) : (
            <ResourceIcon
                fontSize="large"
                sx={{ fontSize: '96px' }}
                color="primary"
            />
        );

    const recordRepresentation =
        typeof definition.recordRepresentation === 'function'
            ? definition.recordRepresentation(record)
            : typeof definition.recordRepresentation === 'string'
            ? definition.recordRepresentation
            : undefined;

    const displayText =
        text || record.name || recordRepresentation || record.id;

    return (
        <>
            {breadcrumb && isValidElement(breadcrumb) ? (
                breadcrumb
            ) : (
                <Link to={createPath({ resource, type: 'list' })}>
                    <Typography
                        variant="body1"
                        mb={2}
                        color="primary"
                        sx={{ fontWeight: 600 }}
                    >
                        {getResourceLabel(resource, 2)} &raquo;
                    </Typography>
                </Link>
            )}
            <PageTitle
                text={displayText}
                secondaryText={record.id}
                copy={true}
                icon={iconEl}
            />
        </>
    );
};
